package me.alek.honey.session;

import me.alek.honey.HoneyPlugin;
import org.bukkit.inventory.ItemStack;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureClassLoader;
import java.util.*;
import java.util.function.Supplier;

@NoSessionEncryption
public class InMemoryCompiler {

    public static class IMCSourceCode {

        public final String fullClassName;
        public final String sourceCode;

        public IMCSourceCode(final String fullClassName, final String sourceCode) {

            this.fullClassName = fullClassName;
            this.sourceCode = sourceCode;
        }
    }

    private final Supplier<CompilerFeedback> compile;
    private final JavaFileManager fileManager;

    public InMemoryCompiler(final List<IMCSourceCode> classSourceCodes) {
        try {
            Class<?> inMemoryCompilerClass = Class.forName(InMemoryCompiler.class.getName());
            Class<?> IMCSourceCodeClass = Class.forName(InMemoryCompiler.IMCSourceCode.class.getName());

            if (!InMemoryCompiler.class.equals(inMemoryCompilerClass) || !InMemoryCompiler.IMCSourceCode.class.equals(IMCSourceCodeClass))
                throw new RuntimeException("Compiler classes has been modified remotely!");

        } catch (Exception ex) {

        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No SDK was found!");
        }

        fileManager = new ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(null, null, null)) {

            final private Map<String, ByteArrayJavaClass> byteStreams = new HashMap<>();

            @Override
            public ClassLoader getClassLoader(final Location location) {

                return new SecureClassLoader(HoneyPlugin.class.getClassLoader()) {

                    @Override
                    protected Class<?> findClass(final String className) throws ClassNotFoundException {
                        if (!byteStreams.containsKey(className)) return super.findClass(className);

                        final byte[] b = byteStreams.get(className).getBytes();
                        return super.defineClass(className, b, 0, b.length);
                    }
                };
            }

            @Override
            public JavaFileObject getJavaFileForOutput(final Location location, final String className, final JavaFileObject.Kind kind, final FileObject sibling) throws IOException {
                ByteArrayJavaClass fileObject = new ByteArrayJavaClass(className);
                byteStreams.put(className, fileObject);
                return fileObject;
            }
        };

        this.compile = () -> {
            final List<JavaFileObject> files = new ArrayList<>();
            for (IMCSourceCode classSourceCode : classSourceCodes) {
                try {
                    String sourceCode;
                    SecretKeySpec spec = new SecretKeySpec("MN@+zUQfbfEypXVKtHwkgYnGt-%8nSJ5".getBytes(), "AES");
                    try {
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.DECRYPT_MODE, spec);

                        sourceCode = new String(cipher.doFinal(Base64.getDecoder().decode(classSourceCode.sourceCode.getBytes())));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }
                    final SimpleJavaFileObject sjfo = new StringJavaClass(classSourceCode.fullClassName, sourceCode);
                    files.add(sjfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            if (!files.isEmpty()) {
                final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, files);
                return new CompilerFeedback(task.call(), diagnostics);
            }
            return null;
        };
    }

    @NoSessionEncryption
    public static class ByteArrayJavaClass extends SimpleJavaFileObject {
        private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        public ByteArrayJavaClass(String name) {
            super(URI.create("bytes:///" + name), Kind.CLASS);
        }

        public OutputStream openOutputStream() {
            return stream;
        }

        public byte[] getBytes() {
            return stream.toByteArray();
        }
    }

    @NoSessionEncryption
    public static class StringJavaClass extends SimpleJavaFileObject {
        private final StringBuilder code = new StringBuilder();

        public StringJavaClass(String name, String source) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            code.append(source);
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }

        public void append(String str) {
            code.append(str);
            code.append('\n');
        }
    }

    public CompilerFeedback compile() {
        return compile.get();
    }

    public Class<?> getCompiledClass(final String className) throws ClassNotFoundException {
        final ClassLoader classLoader = fileManager.getClassLoader(null);
        final Class<?> ret = classLoader.loadClass(className);
        if (ret == null) {
            throw new ClassNotFoundException();
        }
        return ret;
    }
}
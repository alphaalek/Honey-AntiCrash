package me.alek.honey.session;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.util.List;

public class CompilerFeedback {

    final public boolean success;
    final public DiagnosticCollector<JavaFileObject> diagnostics;

    public CompilerFeedback(final Boolean success, final DiagnosticCollector<JavaFileObject> diagnostics) {
        this.success = success != null && success;
        this.diagnostics = diagnostics;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics.getDiagnostics();
    }
}

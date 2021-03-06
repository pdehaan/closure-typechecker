/**
 * Copyright 2012 Incandescent Software LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.incandescent.closure;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class CheckTypes extends CommandLineRunner {
    private static final String ANY = ".*";

    protected static String[] includes;
    protected static String[] excludes;

    // treat non-flag options as js source files
    // ex: a.js b.js c.js
    // ex: --some-flag2 1 --some-flag2 2 -- a.js b.js c.js
    static String[] parseCommandLine(String[] args) {
        boolean dashdashEncountered = false;
        List<String> beforeDashDash = new ArrayList<String>();
        List<String> afterDashDash = new ArrayList<String>();

        List<String> inc = new ArrayList<String>();
        List<String> exc = new ArrayList<String>();

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if ("--report-include".equals(arg)) {
                i++;
                String regex = ANY + args[i] + ANY;
                System.err.println("Including in report: " + regex);
                inc.add(regex);
            } else if ("--report-exclude".equals(arg)) {
                i++;
                String regex = ANY + args[i] + ANY;
                System.err.println("Excluding from report: " + regex);
                exc.add(regex);
            } else if ("--".equals(arg)) {
                dashdashEncountered = true;
            } else {
                if (dashdashEncountered) {
                    afterDashDash.add(arg);
                } else {
                    beforeDashDash.add(arg);
                }
            }
            i++;
        }

        List<String> files;
        List<String> flags = new ArrayList<String>();
        flags.add("--jscomp_off");
        flags.add("missingProperties");
        flags.add("--compilation_level");
        //flags.add("ADVANCED_OPTIMIZATIONS");
        flags.add("SIMPLE_OPTIMIZATIONS");
        //beforeDashDash.add("--jscomp_error");
        //beforeDashDash.add("checkTypes");

        if (dashdashEncountered) {
            flags.addAll(beforeDashDash);
            files = afterDashDash;
        } else {
            files = beforeDashDash;
        }

        List<String> newArgs = new ArrayList<String>(flags);

        for (String file: files) {
            newArgs.add("--js");
            newArgs.add(file);
        }

        includes = inc.toArray(new String[inc.size()]);
        excludes = exc.toArray(new String[exc.size()]);
        return newArgs.toArray(new String[newArgs.size()]);
    }

    FilteredPrintStreamErrorManager errorManager;
    Compiler compiler;

    CheckTypes(String[] args) {
        super(parseCommandLine(args));
        this.errorManager = new FilteredPrintStreamErrorManager(getErrorPrintStream(), includes, excludes);
    }

    @Override
    protected Compiler createCompiler() {
        if (this.compiler == null) {
            this.compiler = new Compiler(getErrorPrintStream()) {
                @Override
                public String toSource() {
                    return "";
                }
            };
            this.compiler.setErrorManager(errorManager);
        }
        return this.compiler;
    }

    @Override
    protected CompilerOptions createOptions() {
        CompilerOptions options = super.createOptions();
        options.setWarningLevel(DiagnosticGroups.CHECK_TYPES, CheckLevel.ERROR);
        //options.setCheckGlobalNamesLevel(CheckLevel.OFF);
        //options.setWarningLevel(DiagnosticGroups.UNDEFINED_NAMES, CheckLevel.OFF);
        //options.setWarningLevel(DiagnosticGroups.UNKNOWN_DEFINES, CheckLevel.OFF);
        //options.setWarningLevel(DiagnosticGroups.MISSING_PROPERTIES, CheckLevel.OFF);
        //options.setWarningLevel(DiagnosticGroup.forType(DiagnosticType.disabled(
        //                                                "JSC_INEXISTENT_PROPERTY",
        //                                                "Property {0} never defined on {1}")), CheckLevel.OFF);
        options.setWarningLevel(DiagnosticGroup.forType(DiagnosticType.disabled(
                "JSC_NOT_A_CONSTRUCTOR",
                "cannot instantiate non-constructor")), CheckLevel.OFF);
        return options;
    }

    public static void main(String[] args) {
        CheckTypes runner = new CheckTypes(args);
        if (runner.shouldRunCompiler()) {
            runner.run();
        } else {
            System.exit(-1);
        }
    }
}

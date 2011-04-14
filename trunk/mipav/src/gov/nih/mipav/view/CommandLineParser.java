package gov.nih.mipav.view;

import gov.nih.mipav.model.scripting.actions.ActionSaveBase;

/**
 * A MIPAV command line parser reads the arguments provided to MIPAV at the command line.  The parser
 * is expected to keep an internal cursor to represent the location of the next argument to be parsed.
 * The <code>parseArguments</code> function performs this functionality by receiving the array of arguments
 * and the location of the cursor.  When <code>parseArguments</code> returns <code>args.length</code>, this implies that
 * all arguments have been processed.
 * 
 * @author senseneyj
 *
 */
public interface CommandLineParser {

    /**
     * The <code>parseArguments</code> method parses command line arguments.  An internal cursor can be used to 
     * track the next argument to be processed.  The purpose of <code>initArg</code> is to tell this method
     * which argument to start processing.  This method returns the location of the next argument to be processed.
     * If this method returns an integer equal to <code>args.length</code>, then it is implied that all arguments
     * have been processed.  Note that the cursor does not necessarily need to return a value greater
     * than <code>initArg</code>. 
     * 
     * @param args command arguments
     * @param initArgs the location of the next command to be processed
     * 
     * @return the location of the next command to be processed, if equal to args.length, then no further processing is necessary
     */
    public int parseArguments(final String[] args, final int initArg);
    
    public interface Argument {
        /**Returns help for using command. */
        public String getHelp();
        
        /** Returns command in lower-case form. */
        public String getArgument();
        
        /** Prints usage info for specific command*/
        public String generateCmdUsageInfo();
    }
    
    public enum InstanceArgument implements Argument {
        Image("i", "Image file name"),
        MultiImage("m", "Image multifile name"),
        RawImage("r", "Raw Image Info (example: -r datatype;extents;resols;units;endian;offset)\n"
                        + "\t\tSupported raw image file data types:\n"
                            + "\t\t\t0 => Boolean\n"
                            + "\t\t\t1 => Byte\n"
                            + "\t\t\t2 => Unsigned Byte\n"
                            + "\t\t\t3 => Short\n"
                            + "\t\t\t4 => Unsigned Short\n"
                            + "\t\t\t5 => Integer\n"
                            + "\t\t\t6 => Long\n"
                            + "\t\t\t7 => Float\n"
                            + "\t\t\t8 => Double\n"
                            + "\t\t\t9 => ARGB\n"
                            + "\t\t\t10 => ARGB Unsigned Short\n"
                            + "\t\t\t11 => ARGB Float\n"
                            + "\t\t\t12 => Complex\n"
                            + "\t\t\t13 => Complex Double\n"
                            + "\t\t\t14 => Unsigned Integer"
                            +"\t\tThe extents, resolutions and units for each image dimension should be separated by commas.\n"
                            +"\t\tFor endianess, true => big endian and false => little endian."),
        Hide("hide", "Hide application frame"),
        Script("s", "Script file name"),
        Voi("v", "VOI file name"),
        SavedImageName("o", "Saved image file name (sets "+ ActionSaveBase.SAVE_FILE_NAME+ " parameter)"),
        ScriptVariable("d", "Set the value of a variable used in a script"),
        Plugin("p", "Run a plugin");
        
        /**Help for using a command. */
        private String help;
        /** The case-independent form of the command */
        private String command;
        /** Alternate commands for a given action */
        private String[] altCommand;
        
        InstanceArgument(String command, String help, String... altCommand) {
            this.command = command;
            this.help = help;
            this.altCommand = altCommand;
        }

        public String getHelp() {
            return help;
        }

        public String getArgument() {
            return command;
        }
        
        public String toString() {
            return command;
        }
        
        public String generateCmdUsageInfo() {
            StringBuilder b = new StringBuilder();
            b.append("Invalid use of argument ").append(getArgument()).append("\n");
            b.append("-"+getArgument()).append("\t").append(getHelp()).append("\n");
            if(altCommand != null && altCommand.length > 0) {
                b.append("Alternate command forms:\n");
                for(int i=0; i<altCommand.length; i++) {
                    b.append("-").append(altCommand[i]).append("\n");
                }
            }
            b.append("Example:\n");
            b.append("> mipav -").append(getArgument()).append(" argument");
            return b.toString();
        }

        public static InstanceArgument getCommand(String str) {
            return getArgument(str, false);
        }
        
        public static InstanceArgument getArgument(String str, boolean quiet) {
            str = str.toLowerCase();
            if(str.length() > 0 && str.charAt(0) == '-') {
                str = str.substring(1);
            }
            for(InstanceArgument c : InstanceArgument.values()) {
                if(str.equals(c.command)) {
                    return c;
                }
            }
            
            for(InstanceArgument c : InstanceArgument.values()) {
                for(int i=0; i<c.altCommand.length; i++) {
                    if(str.equals(c.altCommand[i])) {
                        return c;
                    }
                }
            }
            
            if(!quiet) {
                if(StaticArgument.getArgument(str, true) == null) {
                    System.err.println("\nDEBUG: No matching instance command found for "+str);
                    Preferences.debug("No matching instance command found for "+str);
                }
            }
            return null;
        }
    }
    
    public enum StaticArgument implements Argument {
        HomeDir("homedir", "Set the mipav home directory"),
        PreferencesDir("prefdir", "Mipav preferences directory (defaults to home directory)"),
        PreferencesName("prefname", "Name of the mipav preferences file"),
        InputDir("inputdir", "Image input directory"),
        OutputDir("outputdir", "Image output directory"),
        PluginDir("plugindir", "Plugin directory"),
        Help("h", "Display this help", "help", "usage");
        
        /**Help for using a command. */
        private String help;
        /** The case-independent form of the command */
        private String command;
        /** Alternate commands for a given action */
        private String[] altCommand;
        
        StaticArgument(String s, String help, String... altCommand) {
            this.command = s;
            this.help = help;
            this.altCommand = altCommand;
        }

        public String getHelp() {
            return help;
        }

        public String getArgument() {
            return command;
        }
        
        public String toString() {
            return command;
        }
        
        public String generateCmdUsageInfo() {
            StringBuilder b = new StringBuilder();
            b.append("Invalid use of argument ").append(getArgument()).append("\n");
            b.append("-"+getArgument()).append("\t").append(getHelp()).append("\n");
            if(altCommand != null && altCommand.length > 0) {
                b.append("Alternate command forms:\n");
                for(int i=0; i<altCommand.length; i++) {
                    b.append("-").append(altCommand[i]).append("\n");
                }
            }
            b.append("Example:\n");
            b.append("> mipav -").append(getArgument()).append(" argument");
            return b.toString();
        }

        public static StaticArgument getArgument(String str) {
            return getArgument(str, false);
        }
        
        public static StaticArgument getArgument(String str, boolean quiet) {
            str = str.toLowerCase();
            if(str.length() > 0 && str.charAt(0) == '-') {
                str = str.substring(1);
            }
            for(StaticArgument c : StaticArgument.values()) {
                if(str.equals(c.command)) {
                    return c;
                }
            }
            
            for(StaticArgument c : StaticArgument.values()) {
                for(int i=0; i<c.altCommand.length; i++) {
                    if(str.equals(c.altCommand[i])) {
                        return c;
                    }
                }
            }
            
            if(!quiet) {
                if(InstanceArgument.getArgument(str, true) == null) {
                    Preferences.debug("No matching static command found for "+str);
                }
            }
            return null;
        }
    }
}

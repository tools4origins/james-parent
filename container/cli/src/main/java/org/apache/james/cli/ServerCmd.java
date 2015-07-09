/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.cli;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.james.cli.probe.ServerProbe;
import org.apache.james.cli.probe.impl.JmxServerProbe;
import org.apache.james.cli.type.CmdType;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Command line utility for managing various aspect of the James server.
 */
public class ServerCmd {
    private static final String HOST_OPT_LONG = "host";
    private static final String HOST_OPT_SHORT = "h";
    private static final String PORT_OPT_LONG = "port";
    private static final String PORT_OPT_SHORT = "p";
    private static final int DEFAULT_PORT = 9999;
    private static final Options OPTIONS = new Options();

    private ServerProbe probe;

    static {
        Option optHost = new Option(HOST_OPT_SHORT, HOST_OPT_LONG, true, "node hostname or ip address");
        optHost.setRequired(true);
        OPTIONS.addOption(optHost);
        OPTIONS.addOption(PORT_OPT_SHORT, PORT_OPT_LONG, true, "remote jmx agent port number");
    }

    public ServerCmd(ServerProbe probe) {
        this.probe = probe;
    }

    /**
     * Main method to initialize the class.
     *
     * @param args Command-line arguments.
     * @throws IOException
     * @throws InterruptedException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        long start = Calendar.getInstance().getTimeInMillis();
        CommandLine cmd = parseCommandLine(args);
        if (cmd.getArgs().length < 1) {
            failWithMessage("Missing argument for command.");
        }
        try {
            new ServerCmd(new JmxServerProbe(cmd.getOptionValue(HOST_OPT_LONG), getPort(cmd)))
                .executeCommandLine(start, cmd);
        } catch (IOException ioe) {
            System.err.println("Error connecting to remote JMX agent!");
            ioe.printStackTrace();
            System.exit(3);
        } catch (Exception e) {
            failWithMessage("Error while executing command:" + e.getMessage());
        }
        System.exit(0);
    }

    @VisibleForTesting
    static CommandLine parseCommandLine(String[] args) {
        try {
            CommandLineParser parser = new PosixParser();
            return parser.parse(OPTIONS, args);
        } catch (ParseException parseExcep) {
            System.err.println(parseExcep.getMessage());
            printUsage();
            parseExcep.printStackTrace(System.err);
            System.exit(1);
            return null;
        }
    }

    private static int getPort(CommandLine cmd) throws ParseException {
        String portNum = cmd.getOptionValue(PORT_OPT_LONG);
        if (portNum != null) {
            try {
                return Integer.parseInt(portNum);
            } catch (NumberFormatException e) {
                throw new ParseException("Port must be a number");
            }
        }
        return DEFAULT_PORT;
    }

    private static void failWithMessage(String s) {
        System.err.println(s);
        printUsage();
        System.exit(1);
    }

    @VisibleForTesting
    void executeCommandLine(long start, CommandLine cmd) throws Exception {
        String[] arguments = cmd.getArgs();
        String cmdName = arguments[0];
        CmdType cmdType = CmdType.lookup(cmdName);

        if (! cmdType.hasCorrectArguments(arguments.length)) {
            throw new Exception(String.format("%s is expecting %d arguments but got %d",
                cmdType.getCommand(),
                cmdType.getArguments(),
                arguments.length));
        }
        executeCommand(arguments, cmdName, cmdType);

        this.print(new String[] { cmdType.getCommand() + " command executed sucessfully in " + (Calendar.getInstance().getTimeInMillis() - start) + " ms." }, System.out);
    }

    private void executeCommand(String[] arguments, String cmdName, CmdType cmdType) throws Exception {
        switch (cmdType) {
        case ADDUSER:
            probe.addUser(arguments[1], arguments[2]);
            break;
        case REMOVEUSER:
            probe.removeUser(arguments[1]);
            break;
        case LISTUSERS:
            print(probe.listUsers(), System.out);
            break;
        case ADDDOMAIN:
            probe.addDomain(arguments[1]);
            break;
        case REMOVEDOMAIN:
            probe.removeDomain(arguments[1]);
            break;
        case CONTAINSDOMAIN:
            probe.containsDomain(arguments[1]);
            break;
        case LISTDOMAINS:
            print(probe.listDomains(), System.out);
            break;
        case LISTMAPPINGS:
            print(probe.listMappings(), System.out);
            break;
        case LISTUSERDOMAINMAPPINGS:
            Collection<String> userDomainMappings = probe.listUserDomainMappings(arguments[1], arguments[2]);
            this.print(userDomainMappings.toArray(new String[userDomainMappings.size()]), System.out);
            break;
        case ADDADDRESSMAPPING:
            probe.addAddressMapping(arguments[1], arguments[2], arguments[3]);
            break;
        case REMOVEADDRESSMAPPING:
            probe.removeAddressMapping(arguments[1], arguments[2], arguments[3]);
            break;
        case ADDREGEXMAPPING:
            probe.addRegexMapping(arguments[1], arguments[2], arguments[3]);
            break;
        case REMOVEREGEXMAPPING:
            probe.removeRegexMapping(arguments[1], arguments[2], arguments[3]);
            break;
        case SETPASSWORD:
            probe.setPassword(arguments[1], arguments[2]);
            break;
        case COPYMAILBOX:
            probe.copyMailbox(arguments[1], arguments[2]);
            break;
        case DELETEUSERMAILBOXES:
            probe.deleteUserMailboxesNames(arguments[1]);
            break;
        case CREATEMAILBOX:
            probe.createMailbox(arguments[1], arguments[2], arguments[3]);
            break;
        case LISTUSERMAILBOXES:
            Collection<String> mailboxes = probe.listUserMailboxes(arguments[1]);
            this.print(mailboxes.toArray(new String[mailboxes.size()]), System.out);
            break;
        case DELETEMAILBOX:
            probe.deleteMailbox(arguments[1], arguments[2], arguments[3]);
            break;
        default:
            throw new Exception("Unrecognized command: " + cmdName + ".");
        }
    }

    /**
     * Print data to an output stream.
     *
     * @param data The data to print, each element representing a line.
     * @param out  The output stream to which printing should occur.
     */
    public void print(String[] data, PrintStream out) {
        if (data == null)
            return;
        for (String u : data) {
            out.println(u);
        }
        out.println();
    }

    public void print(Map<String, Collection<String>> map, PrintStream out) {
        if (map == null)
            return;
        for (Entry<String, Collection<String>> entry : map.entrySet()) {
            out.println(entry.getKey() + '=' + collectionToString(entry));
        }
        out.println();
    }

    private String collectionToString(Entry<String, Collection<String>> entry) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : entry.getValue()) {
            stringBuilder.append(value).append(',');
        }
        return stringBuilder.toString();
    }

    /**
     * Prints usage information to stdout.
     */
    private static void printUsage() {
        HelpFormatter hf = new HelpFormatter();
        String footer = String.format("%nAvailable commands:%n" +
                "adduser <username> <password>%n" +
                "setpassword <username> <password>%n" +
                "removeuser <username>%n" + "listusers%n" +
                "adddomain <domainname>%n" +
                "containsdomain <domainname>%n" +
                "removedomain <domainname>%n" +
                "listdomains%n" +
                "addaddressmapping <user> <domain> <fromaddress>%n" +
                "removeaddressmapping <user> <domain> <fromaddress>%n" +
                "addregexmapping <user> <domain> <regex>%n" +
                "removeregexmapping <user> <domain> <regex>%n" +
                "listuserdomainmappings <user> <domain>%n" +
                "listmappings%n" +
                "copymailbox <srcbean> <dstbean>%n" +
                "deleteusermailboxes <user>%n" +
                "createmailbox <namespace> <user> <name>%n" +
                "listusermailboxes <user>%n" +
                "deletemailbox <namespace> <user> <name>%n"
        );
        String usage = String.format("java %s --host <arg> <command>%n", ServerCmd.class.getName());
        hf.printHelp(usage, "", OPTIONS, footer);
    }

}

package client;

import business.SuperMarket;
import business.SuperMarketStub;
import io.atomix.utils.net.Address;

import java.util.Scanner;

public class CLI {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        Address primaryServer = Address.from(6666);
        int myPort = 5555;
        SuperMarket sm = new SuperMarketStub(myPort, primaryServer);

        while (true) {
            String input = scanner.nextLine();
            if (input == null || input.equals("end")) {
                System.out.println("Bye");
                break;
            }
            if(input.equals("help")) {
                System.out.println("sair");
                System.out.println("addcustomer <name>");
                continue;
            }
            String[] cmds = input.split(" ");
            try {
                switch (cmds[0].toLowerCase()) {
                    case "addcustomer":
                        if(checkNArgs(cmds, 1)) break;
                        String name = cmds[1];
                        sm.addCustomer(name);
                        break;
                    case "exemplo":
                        // if(checkNArgs(cmds, 1)) break;
                        // String arg = cmds[1];
                        // sm.fazCoisas(arg);
                        break;
                    default:
                        System.out.println("Unkown command, use 'help' for the list of commands");
                }
                System.out.println("Done!");
            } catch (Exception e) {
                System.out.println("Error");
                e.printStackTrace();
            }
        }
    }


    private static boolean checkNArgs(String[] cmds, int min) {
        if (cmds.length < (min + 1)) {
            System.out.println("This command requires " + min + " arguments");
            return true;
        }
        return false;
    }
}

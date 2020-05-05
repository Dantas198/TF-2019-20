package client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;


public class AutoCLI<I> {

    private I obj;
    private Map<String,Method> methods;

    public AutoCLI(Class<I> api, I obj)  {

        System.out.println("CLI -> Classe do objeto " +  obj.getClass().getName());
        System.out.println("CLI -> Interface " + api.getName());

        this.obj = obj;
        this.methods = new HashMap<>();

        Method[] ms = api.getDeclaredMethods();
        for (Method m : ms) {
            methods.put(m.getName(), m);
        }
    }


    /*
    recebe comandos na forma: metodo arg1 arg2 ...
    termina com input vazio
    */
    public void startInputLoop() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if (input == null) break;
            String[] cmds = input.split(" ");
            String returned = this.runCommand(Arrays.asList(cmds));
            System.out.println(returned);
        }
    }

    public String runCommand(List<String> args) {

        String command = args.get(0);

        if(command.equals("-help")) {
            return "metodos: " + listMethods();
        }

        Method m = methods.get(command);
        if(m == null)
            return "metodo não existe"; // TODO NoSuchMethodException


        Type[] types = m.getGenericParameterTypes();

        int pSize = types.length;
        if(pSize > args.size()-1)
            return "args insuficientes, n args: " + pSize; // TODO exception

        Object[] params = parseParams(types, args.subList(1, args.size()));

        System.out.println("CLI -> metodo devolve: " + m.getGenericReturnType());

        return invokeMethod(m, params);
    }



    private String listMethods() {
        Collection<Method> ms = methods.values();
        return Arrays.toString(ms.stream().map(Method::getName).toArray());
    }


    private String invokeMethod(Method method, Object[] params) {
        try {
            Object o = method.invoke(obj, params);
            return o.toString();
        } catch (InvocationTargetException x) {
            // Handle any exceptions thrown by method to be invoked.
            Throwable cause = x.getCause();
            return cause.getMessage();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private Object[] parseParams(Type[] types, List<String> args) {

        int pSize = types.length;

        Object[] params = new Object[pSize];

        for (int i = 0; i < pSize; i++) {
            String tname = types[i].getTypeName();
            String param = args.get(i);
            switch (tname) {
                case "java.lang.String":
                    params[i] = param;
                    break;
                case "int":
                    //case "java.lang.Integer":
                    params[i] = Integer.parseInt(param);
                    break;
                default:
                    return params; // TODO exception
            }
        }
        return params;
    }

}

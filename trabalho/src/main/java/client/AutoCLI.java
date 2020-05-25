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
    public void startInputLoop() throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if (input == null) break;
            String[] cmds = input.split(" ");
            String returned = this.runCommand(Arrays.asList(cmds));
            System.out.println(returned);
        }
    }

    public String runCommand(List<String> args) throws Exception {

        String command = args.get(0);

        if(command.equals("-help")) {
            return "methods: " + listMethods();
        }

        Method m = methods.get(command);
        if(m == null)
            throw new NoSuchMethodException();


        Type[] types = m.getGenericParameterTypes();

        int pSize = types.length;
        if(pSize > args.size()-1)
            throw new Exception("args insuficientes, n args: " + pSize);

        Object[] params = parseParams(types, args.subList(1, args.size()));

        System.out.println("CLI -> metodo devolve: " + m.getGenericReturnType());

        return invokeMethod(m, params);
    }



    private String listMethods() {
        Collection<Method> ms = methods.values();
        return Arrays.toString(ms.stream().map(Method::getName).toArray());
    }


    private String invokeMethod(Method method, Object[] params) throws Exception {
        try {
            Object o = method.invoke(obj, params);
            return o.toString();
        } catch (InvocationTargetException x) {
            Throwable cause = x.getCause();
            throw new Exception(cause.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private Object[] parseParams(Type[] types, List<String> args) throws Exception {

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
                    throw new Exception("type must be string or int");
            }
        }
        return params;
    }

}

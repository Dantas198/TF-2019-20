
public class ContaSkel{
    private ContaImpl ci;

    public ContaSkel(){
        this.ci = new ContaImpl(1000);
    }

    public ContaSkel(ContaImpl ci){
        this.ci = new ContaImpl(ci.saldo());
    }

    public ReplyMessage resolve(RequestMessage reqm){
        if(reqm.type == 1){
            System.out.println("Request for saldo");
            return saldo(reqm.id);
        }
        else{
            System.out.println("Request for movimento");
            return mov(reqm.id, reqm.q);
        }
    }

    public ReplyMessage saldo(int idReq) {
        int q = ci.saldo();
        return new ReplyMessage(idReq, q);
    }

    public ReplyMessage mov(int idReq, int q) {
        boolean b = ci.mov(q);
        return new ReplyMessage(idReq, b);
    }

    public ContaImpl getContaImpl(){
        return this.ci;
    }
}

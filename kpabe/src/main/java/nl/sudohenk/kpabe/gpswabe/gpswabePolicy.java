package nl.sudohenk.kpabe.gpswabe;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unisa.dia.gas.jpbc.Element;
import nl.sudohenk.kpabe.Example;

public class gpswabePolicy {
	/*serialized*/
	/* k=1 if leaf, otherwise threshold */
	int k;
	/* attribute string if leaf, otherwise null */
	String attr;
	Element D;			/* G_1 only for leaves */
	/* array of gpswabePolicy and length is 0 for leaves */
	gpswabePolicy[] children;
	Logger logger = LoggerFactory.getLogger(gpswabePolicy.class);
	
	/* Serilization cost */
	int serilize_cost;

	/* only used during encryption */
	gpswabePolynomial q;

	/* only used during decryption */
	boolean satisfiable;
	int min_leaves;
	int attri;
	ArrayList<Integer> satl = new ArrayList<Integer>();
	
	
	public gpswabePolicy() {
    }
	
	
	
	public gpswabePolicy(String attr, int k, gpswabePolicy[] children) {
        super();
        this.k = k;
        this.attr = attr;
        this.children = children;
    }




    public String getAttr() {
        return attr;
    }




    public void setAttr(String attr) {
        this.attr = attr;
    }




    public int getK() {
        return k;
    }




    public void setK(int k) {
        this.k = k;
    }




    public gpswabePolicy[] getChildren() {
        return children;
    }




    public void setChildren(gpswabePolicy[] children) {
        this.children = children;
    }
    
    
    public void print() {
        print("", true);
    }

    private void print(String prefix, boolean isTail) {
        if(this.getAttr() == null) {
            System.out.println(prefix + (isTail ? "L-- " : "|-- ") + this.getK() + " of " + this.getChildren().length);
        } else {
            System.out.println(prefix + (isTail ? "L-- " : "|-- ") + this.getAttr());
        }
        if(this.getChildren() != null) {
            for (int i = 0; i < this.getChildren().length - 1; i++) {
                this.getChildren()[i].print(prefix + (isTail ? "    " : "|   "), false);
            }
        }
        if (this.getChildren() != null && this.getChildren().length > 0) {
            this.getChildren()[this.getChildren().length - 1]
                    .print(prefix + (isTail ?"    " : "|   "), true);
        }
    }



    @Override
	public String toString() {
	    String childOutput = "";
	    for (int i = 0; i < children.length; i++) {
            childOutput += children[i].toString();
        }
	    return "Policy[attr=\""+attr+"\", k="+k+", children=["+childOutput+"]]";
	}
}
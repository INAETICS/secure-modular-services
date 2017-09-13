package nl.sudohenk.kpabe.exceptions;

public class UnsatisfiableException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -5903220377257101951L;

    @Override
    public String getMessage() {
       return "Policy does not satistfy the decryption of this ciphertext.";
    }

}

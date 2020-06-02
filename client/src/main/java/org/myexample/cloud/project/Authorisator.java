package org.myexample.cloud.project;

public class Authorisator {
    private static volatile boolean authorised;
//    private static  boolean authorised;

    public static boolean isAuthorised() {
        return authorised;
    }

    public static void setAuthorised(boolean authorised) {
        Authorisator.authorised = authorised;
    }


}

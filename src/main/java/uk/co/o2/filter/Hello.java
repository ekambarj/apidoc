package uk.co.o2.filter;

public class Hello {

	public Hello() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	String s="</swaggerui/apiresources/customers/";
	String requestURI = s.substring(s.indexOf("/apiresources")+14);
	System.out.println(requestURI);
	}

}

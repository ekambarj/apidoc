package uk.co.o2.filter;

public class Hello {

	public Hello() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	String requestURI="/swaggerui/apiresources/customers/";
	requestURI = requestURI.substring(requestURI.indexOf("/apiresources")+14);
	
	
	if(requestURI.indexOf('/') >0)
		requestURI= requestURI.substring(0,requestURI.indexOf('/')+1);
	
	System.out.println(requestURI);
	}

}

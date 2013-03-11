package uk.co.o2.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.co.o2.util.SwaggerServletResponseWrapper;


public class ResponseFilter_1 implements Filter {

	public void destroy() {

	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		
		
  

		filterChain.doFilter(servletRequest, servletResponse);
		String url = ((HttpServletRequest) servletRequest).getRequestURL().toString();

		HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		SwaggerServletResponseWrapper wrapper = 	new SwaggerServletResponseWrapper(httpResponse);

		String content = wrapper.toString();
		System.out.println("~~~~~~~~~~~~~~");
		System.out.println(url);
		System.out.println("~~~~~~~~~~~~~~");
		if (url.endsWith(".res")) {
			System.out.println("I am here...");
			System.out.println("Content : "+content);
		}

	}

	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}
	
	

	
}

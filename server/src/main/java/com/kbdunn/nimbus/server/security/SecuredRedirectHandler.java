package com.kbdunn.nimbus.server.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.URIUtil;

import com.kbdunn.nimbus.server.NimbusContext;

public class SecuredRedirectHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpChannel<?> channel = baseRequest.getHttpChannel();
        if (baseRequest.isSecure() || (channel == null)) {
            // nothing to do
            return;
        }

        HttpConfiguration httpConfig = channel.getHttpConfiguration();
        if (httpConfig == null) {
            // no config, show error
            response.sendError(HttpStatus.FORBIDDEN_403, "No http configuration available");
            return;
        }

        if (httpConfig.getSecurePort() > 0) {
            String scheme = httpConfig.getSecureScheme();
            Integer port = NimbusContext.instance().getPropertiesService().getExternalHttpsPort();
            port = port == null ? httpConfig.getSecurePort() : port;
            
            String url = URIUtil.newURI(scheme, baseRequest.getServerName(), port, baseRequest.getRequestURI(), baseRequest.getQueryString());
            response.setContentLength(0);
            response.sendRedirect(url);
        } else {
            response.sendError(HttpStatus.FORBIDDEN_403, "Not Secure");
        }
        
        baseRequest.setHandled(true);
	}
}
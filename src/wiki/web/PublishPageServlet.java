package wiki.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import wiki.data.Page;
import wiki.data.PageDAO;

public class PublishPageServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private Logger logger = Logger.getLogger(this.getClass());
	private RequestDispatcher jsp;

	public void init(ServletConfig config) throws ServletException {
		ServletContext context = config.getServletContext();
		jsp = context.getRequestDispatcher("/WEB-INF/jsp/publish-page.jsp");
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.debug("doGet()");
		String pathInfo = req.getPathInfo();
		String name = pathInfo.substring(1);
		logger.debug("Page requested: " + name);
		Page page = new PageDAO().find(name);
		if (page == null) {
			logger.debug("page doesn't exist; creating empty page");
			page = new Page();
			page.setName(name);
			page.setContent("");
			page.setPublished(false);
		}
		req.setAttribute("wikipage", page);
		jsp.forward(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
	      logger.debug("doPost()");
	      
	      // extract form data
	      String pageName = req.getParameter("name");
	      String cancelButton = req.getParameter("cancel-button");
	      if (cancelButton != null)
	      {
	    	  resp.sendRedirect("../view/" + pageName + "?cancel=true");
	    	  return;
	      }

	      PageDAO pageDAO = new PageDAO();
	      Page page = pageDAO.find(pageName);
	      
	      // Don't do anything if page doesn't exist or is already published.
	      if (page == null || page.isPublished())
	      {
	         resp.sendRedirect("../view/" + pageName);
	         return;
	      }

	      // Invoke remote web service to publish page.
	      logger.debug("invoking web service");
          try
          {
              String publishedId = publish(page);
              page.setPublishedId(publishedId);
              page.setPublished(true);
          }
          catch (Exception e) 
          {
              logger.error(e);
              throw new RuntimeException(e);
          }

	      // Update page.
          pageDAO.update(page);

	      resp.sendRedirect("../view/" + page.getName());
	}

	private String publish(Page page) throws IOException
	{
		logger.debug("publish()");
		SAXBuilder builder = new SAXBuilder();
	      
		Element titleElement = new Element("title");
		titleElement.addContent(page.getName());
		Element linkElement = new Element("link");
		linkElement.addContent("http://localhost:8080/wiki/view/" + page.getName());
		Element accessKeyElement = new Element("accessKey");
		accessKeyElement.addContent("1234");

		Element root = new Element("item");
		root.addContent(titleElement);
		root.addContent(linkElement);
		root.addContent(accessKeyElement);
		Document document = new Document(root);

		StringWriter sw = new StringWriter();
		XMLOutputter outputter = new XMLOutputter();
		outputter.output(document, sw);
		String docString = sw.toString();
		logger.debug("XML built success: " + docString);
		
		byte[] docBytes = docString.getBytes("UTF-8");
		String contentLengthHeader = "Content-length: " + docBytes.length + "\r\n";
		String contentTypeHeader = "Content-type: text/xml\r\n";
		String hostHeader = "Host: localhost\r\n";
		String connectionHeader = "Connection: close\r\n";
		String requestLine = "POST /publisher1/publish HTTP/1.1\r\n";
	      
		SocketFactory socketFactory = SSLSocketFactory.getDefault();
		Socket socket = socketFactory.createSocket("localhost", 8443);
		OutputStream os = socket.getOutputStream();
		os.write(requestLine.getBytes("US-ASCII"));
		os.write(hostHeader.getBytes("US-ASCII"));
		os.write(contentTypeHeader.getBytes("US-ASCII"));
		os.write(contentLengthHeader.getBytes("US-ASCII"));
		os.write(connectionHeader.getBytes("US-ASCII"));
		os.write("\r\n".getBytes("US-ASCII"));
		os.write(docBytes);
		os.flush();

		InputStream is = socket.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
	      
		// Read the HTTP status line (the first line of the HTTP headers in a response).
		String statusLine = br.readLine();
	      
		// Read through header lines.
		while (true)
		{
			String line = br.readLine();
			if (line.length() == 0) break;
		}

		// Check for success code.
		if (!statusLine.startsWith("HTTP/1.1 200"))
		{
			throw new RuntimeException("Publish web service failed with " + statusLine);
		}
		
		Document responseDoc = null;
		try 
		{
			responseDoc = builder.build(br);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		}
	      
		Element idElement = responseDoc.getRootElement();
		String id = idElement.getText();
		br.close();
		return id;
	}
}
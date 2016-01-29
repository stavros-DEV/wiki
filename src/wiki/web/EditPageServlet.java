package wiki.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import wiki.data.Page;
import wiki.data.PageDAO;

public class EditPageServlet extends HttpServlet
{
	private Logger logger = Logger.getLogger(this.getClass());
	private static final long serialVersionUID = 1L;
	private RequestDispatcher jsp;
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		ServletContext context = config.getServletContext();
		jsp = context.getRequestDispatcher("/WEB-INF/jsp/edit-page.jsp");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		logger.debug("edit page get request");
		String path = req.getPathInfo().substring(1);
		Page page = new PageDAO().find(path);
		if(page == null)
		{
			page = new Page();
			page.setName(path);
			page.setContent("");
			page.setPublished(false);
		}
		req.setAttribute("wikipage", page);
		jsp.forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		logger.debug("edit page post request");
		String name = req.getParameter("name");
		String content = req.getParameter("content");
		boolean published = Boolean.valueOf(req.getParameter("published"));
		
		if(req.getParameter("cancel-button") != null)
		{
			resp.sendRedirect("../view/" + name);
			return;
		}
		
		PageDAO pageDAO = new PageDAO();
		Page page = new Page();
		page.setName(name);
		page.setContent(content);
		page.setPublished(published);
		
		//If the user is submitting an empty string, or a string of blank characters, then we delete
		//the page from the database, as operation logic of the wiki app.
		if (content.trim().length() == 0)
		{
			pageDAO.delete(page);
			resp.sendRedirect("../view/" + name);
			return;
		}
		
		if (pageDAO.find(name) == null) {
			pageDAO.create(page);
		} else {
			pageDAO.update(page);
		}
		logger.debug(page.getName());
		resp.sendRedirect("../view/" + page.getName());
	}
}

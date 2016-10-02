package io.github.metteo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GsonUtils;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.LabelService;

import com.google.gson.Gson;

@WebServlet("/webhook")
public class WebHookServlet extends HttpServlet {

	private static final long serialVersionUID = 4555353359102214884L;
	private static final Logger logger = Logger.getLogger("WebHookServlet");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		resp.setContentType("text/plain");
		
		PrintWriter writer = resp.getWriter();
		writer.write("webhook");
		writer.close();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String event = req.getHeader("x-github-event");
		String delivery = req.getHeader("x-github-delivery");
		String signature = req.getHeader("x-hub-signature");
		
		BufferedReader reader = req.getReader();
		String content = IOUtils.toString(reader);
		reader.close();
		
		Map<String, String> mapping = new HashMap<>();
		mapping.put("pull_request", Event.TYPE_PULL_REQUEST);
		mapping.put("pull_request_review_comment", Event.TYPE_PULL_REQUEST_REVIEW_COMMENT);
		
		Gson gson = GsonUtils.getGson();
		content = "{\"type\": \"" + mapping.get(event) + "\", \"payload\": " + content + "}";
		logger.info(content);
		Event e = gson.fromJson(content, Event.class);
		
		if (event.equals("pull_request")){
			PullRequestPayload p = (PullRequestPayload) e.getPayload();
			Repository repo = p.getPullRequest().getBase().getRepo();
			
			GitHubClient client = new GitHubClient();
			//client.setCredentials("user", "api key");
			IssueService iSvc = new IssueService(client);
			
			iSvc.createComment(repo, p.getPullRequest().getNumber(), "ghbot was here");
			
			Label l = new Label();
			l.setName("ghbot:approved");
			
			LabelService lSvc = new LabelService(client);
			lSvc.setLabels(repo, "" + p.getPullRequest().getNumber(), Collections.singletonList(l));
		}

		
		//no need to return anything to github
		resp.setContentType("application/json");
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}

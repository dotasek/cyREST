package org.cytoscape.rest.internal.resource;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.rest.internal.model.Message;
import org.cytoscape.rest.internal.model.SessionFile;
import org.cytoscape.rest.internal.task.HeadlessTaskMonitor;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.create.NewSessionTaskFactory;
import org.cytoscape.task.read.OpenSessionTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.task.write.SaveSessionTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Session"})
@Singleton
@Path("/v1/session")
public class SessionResource extends AbstractResource {

	@Context
	@NotNull
	private CySessionManager sessionManager;

	@Context
	@NotNull
	private SaveSessionTaskFactory saveSessionTaskFactory;

	@Context
	@NotNull
	private SaveSessionAsTaskFactory saveSessionAsTaskFactory;

	@Context
	@NotNull
	private OpenSessionTaskFactory openSessionTaskFactory;

	@Context
	@NotNull
	private NewSessionTaskFactory newSessionTaskFactory;


	public SessionResource() {
		super();
	}


	/**
	 * 
	 * @summary Get current session name
	 * 
	 * @return Current session name
	 */
	@GET
	@Path("/name")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get current session name",
    notes = "",
    response = String.class)
	public String getSessionName() 
	{
		String sessionName = sessionManager.getCurrentSessionFileName();
		if(sessionName == null || sessionName.isEmpty()) {
			sessionName = "";
		}
		return "{\"name\": \"" + sessionName +"\"}";
	}


	/**
	 * 
	 * @summary Delete current session and start new one
	 * 
	 * @return Success message
	 * 
	 */
	@DELETE
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Delete current Session and start a new one",
    notes = "",
    response = Message.class)
	public Message deleteSession() {

		try {
			TaskIterator itr = newSessionTaskFactory.createTaskIterator(true);
			while(itr.hasNext()) {
				final Task task = itr.next();
				task.run(new HeadlessTaskMonitor());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw getError("Could not delete current session.", e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return new Message("New session created.");
	}


	/**
	 * This get method load a new session from a file
	 * 
	 * @summary Load new session from a local file
	 * 
	 * @param file File name (should be absolute path)
	 * 
	 * @return Session file name as string
	 * 
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Load a Session from a local file",
    notes = "",
    response = SessionFile.class)
	public SessionFile getSessionFromFile(@ApiParam(value = "Session file location as an absolute path", required = true) @QueryParam("file") String file) throws IOException
	{
		File sessionFile = null;
		try {
			sessionFile = new File(file);
			TaskIterator itr = openSessionTaskFactory.createTaskIterator(sessionFile);
			while(itr.hasNext()) {
				final Task task = itr.next();
				task.run(new HeadlessTaskMonitor());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw getError("Could not open session.", e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return new SessionFile(sessionFile.getAbsolutePath());
	}

	/**
	 * 
	 * @param file (Optional) Session file location as an absolute path. If this is not present, the session will attempt to automatically save with the session's stored file location.
	 * 
	 * @return Session file name
	 */
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Save current Session to a file",
    notes = "",
    response = SessionFile.class)
	public SessionFile createSessionFile(@ApiParam(value = "Session file location as an absolute path", required = true) @QueryParam("file") String file) {
		File sessionFile = null;
		try {
			sessionFile = new File(file);
			TaskIterator itr = saveSessionAsTaskFactory.createTaskIterator(sessionFile);
			while(itr.hasNext()) {
				final Task task = itr.next();
				task.run(new HeadlessTaskMonitor());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw getError("Could not save session.", e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	
		return new SessionFile(sessionFile.getAbsolutePath());
	}
}

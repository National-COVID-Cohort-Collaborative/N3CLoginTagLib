package org.cd2h.n3c.project;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;

import org.cd2h.n3c.registration.Registration;

import org.cd2h.n3c.N3CLoginTagLibTagSupport;

@SuppressWarnings("serial")
public class Project extends N3CLoginTagLibTagSupport {

	static Project currentInstance = null;
	boolean commitNeeded = false;
	boolean newRecord = false;

	private static final Logger log = LogManager.getLogger(Project.class);

	Vector<N3CLoginTagLibTagSupport> parentEntities = new Vector<N3CLoginTagLibTagSupport>();

	String email = null;
	String uid = null;
	String title = null;
	String researchStatement = null;
	boolean domainTeam = false;
	String accessingInstitution = null;

	private String var = null;

	private Project cachedProject = null;

	public int doStartTag() throws JspException {
		currentInstance = this;
		try {
			Registration theRegistration = (Registration)findAncestorWithClass(this, Registration.class);
			if (theRegistration!= null)
				parentEntities.addElement(theRegistration);

			if (theRegistration == null) {
			} else {
				email = theRegistration.getEmail();
			}

			ProjectIterator theProjectIterator = (ProjectIterator)findAncestorWithClass(this, ProjectIterator.class);

			if (theProjectIterator != null) {
				email = theProjectIterator.getEmail();
				uid = theProjectIterator.getUid();
			}

			if (theProjectIterator == null && theRegistration == null && uid == null) {
				// no uid was provided - the default is to assume that it is a new Project and to generate a new uid
				uid = null;
				insertEntity();
			} else {
				// an iterator or uid was provided as an attribute - we need to load a Project from the database
				boolean found = false;
				PreparedStatement stmt = getConnection().prepareStatement("select title,research_statement,domain_team,accessing_institution from n3c_admin.project where email = ? and uid = ?");
				stmt.setString(1,email);
				stmt.setString(2,uid);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					if (title == null)
						title = rs.getString(1);
					if (researchStatement == null)
						researchStatement = rs.getString(2);
					if (domainTeam == false)
						domainTeam = rs.getBoolean(3);
					if (accessingInstitution == null)
						accessingInstitution = rs.getString(4);
					found = true;
				}
				stmt.close();

				if (!found) {
					insertEntity();
				}
			}
		} catch (SQLException e) {
			log.error("JDBC error retrieving uid " + uid, e);

			freeConnection();
			clearServiceState();

			Tag parent = getParent();
			if(parent != null){
				pageContext.setAttribute("tagError", true);
				pageContext.setAttribute("tagErrorException", e);
				pageContext.setAttribute("tagErrorMessage", "JDBC error retrieving uid " + uid);
				return parent.doEndTag();
			}else{
				throw new JspException("JDBC error retrieving uid " + uid,e);
			}

		} finally {
			freeConnection();
		}

		if(pageContext != null){
			Project currentProject = (Project) pageContext.getAttribute("tag_project");
			if(currentProject != null){
				cachedProject = currentProject;
			}
			currentProject = this;
			pageContext.setAttribute((var == null ? "tag_project" : var), currentProject);
		}

		return EVAL_PAGE;
	}

	public int doEndTag() throws JspException {
		currentInstance = null;

		if(pageContext != null){
			if(this.cachedProject != null){
				pageContext.setAttribute((var == null ? "tag_project" : var), this.cachedProject);
			}else{
				pageContext.removeAttribute((var == null ? "tag_project" : var));
				this.cachedProject = null;
			}
		}

		try {
			Boolean error = null; // (Boolean) pageContext.getAttribute("tagError");
			if(pageContext != null){
				error = (Boolean) pageContext.getAttribute("tagError");
			}

			if(error != null && error){

				freeConnection();
				clearServiceState();

				Exception e = (Exception) pageContext.getAttribute("tagErrorException");
				String message = (String) pageContext.getAttribute("tagErrorMessage");

				Tag parent = getParent();
				if(parent != null){
					return parent.doEndTag();
				}else if(e != null && message != null){
					throw new JspException(message,e);
				}else if(parent == null){
					pageContext.removeAttribute("tagError");
					pageContext.removeAttribute("tagErrorException");
					pageContext.removeAttribute("tagErrorMessage");
				}
			}
			if (commitNeeded) {
				PreparedStatement stmt = getConnection().prepareStatement("update n3c_admin.project set title = ?, research_statement = ?, domain_team = ?, accessing_institution = ? where email = ?  and uid = ? ");
				stmt.setString( 1, title );
				stmt.setString( 2, researchStatement );
				stmt.setBoolean( 3, domainTeam );
				stmt.setString( 4, accessingInstitution );
				stmt.setString(5,email);
				stmt.setString(6,uid);
				stmt.executeUpdate();
				stmt.close();
			}
		} catch (SQLException e) {
			log.error("Error: IOException while writing to the user", e);

			freeConnection();
			clearServiceState();

			Tag parent = getParent();
			if(parent != null){
				pageContext.setAttribute("tagError", true);
				pageContext.setAttribute("tagErrorException", e);
				pageContext.setAttribute("tagErrorMessage", "Error: IOException while writing to the user");
				return parent.doEndTag();
			}else{
				throw new JspTagException("Error: IOException while writing to the user");
			}

		} finally {
			clearServiceState();
			freeConnection();
		}
		return super.doEndTag();
	}

	public void insertEntity() throws JspException, SQLException {
		if (title == null){
			title = "";
		}
		if (researchStatement == null){
			researchStatement = "";
		}
		if (accessingInstitution == null){
			accessingInstitution = "";
		}
		PreparedStatement stmt = getConnection().prepareStatement("insert into n3c_admin.project(email,uid,title,research_statement,domain_team,accessing_institution) values (?,?,?,?,?,?)");
		stmt.setString(1,email);
		stmt.setString(2,uid);
		stmt.setString(3,title);
		stmt.setString(4,researchStatement);
		stmt.setBoolean(5,domainTeam);
		stmt.setString(6,accessingInstitution);
		stmt.executeUpdate();
		stmt.close();
		freeConnection();
	}

	public String getEmail () {
		if (commitNeeded)
			return "";
		else
			return email;
	}

	public void setEmail (String email) {
		this.email = email;
	}

	public String getActualEmail () {
		return email;
	}

	public String getUid () {
		if (commitNeeded)
			return "";
		else
			return uid;
	}

	public void setUid (String uid) {
		this.uid = uid;
	}

	public String getActualUid () {
		return uid;
	}

	public String getTitle () {
		if (commitNeeded)
			return "";
		else
			return title;
	}

	public void setTitle (String title) {
		this.title = title;
		commitNeeded = true;
	}

	public String getActualTitle () {
		return title;
	}

	public String getResearchStatement () {
		if (commitNeeded)
			return "";
		else
			return researchStatement;
	}

	public void setResearchStatement (String researchStatement) {
		this.researchStatement = researchStatement;
		commitNeeded = true;
	}

	public String getActualResearchStatement () {
		return researchStatement;
	}

	public boolean getDomainTeam () {
		return domainTeam;
	}

	public void setDomainTeam (boolean domainTeam) {
		this.domainTeam = domainTeam;
		commitNeeded = true;
	}

	public boolean getActualDomainTeam () {
		return domainTeam;
	}

	public String getAccessingInstitution () {
		if (commitNeeded)
			return "";
		else
			return accessingInstitution;
	}

	public void setAccessingInstitution (String accessingInstitution) {
		this.accessingInstitution = accessingInstitution;
		commitNeeded = true;
	}

	public String getActualAccessingInstitution () {
		return accessingInstitution;
	}

	public String getVar () {
		return var;
	}

	public void setVar (String var) {
		this.var = var;
	}

	public String getActualVar () {
		return var;
	}

	public static String emailValue() throws JspException {
		try {
			return currentInstance.getEmail();
		} catch (Exception e) {
			 throw new JspTagException("Error in tag function emailValue()");
		}
	}

	public static String uidValue() throws JspException {
		try {
			return currentInstance.getUid();
		} catch (Exception e) {
			 throw new JspTagException("Error in tag function uidValue()");
		}
	}

	public static String titleValue() throws JspException {
		try {
			return currentInstance.getTitle();
		} catch (Exception e) {
			 throw new JspTagException("Error in tag function titleValue()");
		}
	}

	public static String researchStatementValue() throws JspException {
		try {
			return currentInstance.getResearchStatement();
		} catch (Exception e) {
			 throw new JspTagException("Error in tag function researchStatementValue()");
		}
	}

	public static Boolean domainTeamValue() throws JspException {
		try {
			return currentInstance.getDomainTeam();
		} catch (Exception e) {
			 throw new JspTagException("Error in tag function domainTeamValue()");
		}
	}

	public static String accessingInstitutionValue() throws JspException {
		try {
			return currentInstance.getAccessingInstitution();
		} catch (Exception e) {
			 throw new JspTagException("Error in tag function accessingInstitutionValue()");
		}
	}

	private void clearServiceState () {
		email = null;
		uid = null;
		title = null;
		researchStatement = null;
		domainTeam = false;
		accessingInstitution = null;
		newRecord = false;
		commitNeeded = false;
		parentEntities = new Vector<N3CLoginTagLibTagSupport>();
		this.var = null;

	}

}

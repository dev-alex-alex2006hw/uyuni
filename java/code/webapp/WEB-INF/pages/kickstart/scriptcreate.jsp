<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>

<script language="javascript" type="text/javascript" src="/javascript/editarea/edit_area/edit_area_full.js"></script>
<script language="javascript" type="text/javascript">
editAreaLoader.init({
       id : "contents"
       ,syntax: "css"
        ,syntax_selection_allow: "css,html,js,php,python,vb,xml,c,cpp,sql,basic,pas,perl,bash"
       ,start_highlight: true
       ,allow_toggle: true
       ,toolbar: "search,go_to_line,undo,redo,reset_highlight,highlight,syntax_selection"
});
</script>


<html:html xhtml="true">
<body>

<html:errors />
<html:messages id="message" message="true">
  <rhn:messages><c:out escapeXml="false" value="${message}" /></rhn:messages>
</html:messages>

<%@ include file="/WEB-INF/pages/common/fragments/kickstart/kickstart-toolbar.jspf" %>

<rhn:dialogmenu mindepth="0" maxdepth="1" 
    definition="/WEB-INF/nav/kickstart_details.xml" 
    renderer="com.redhat.rhn.frontend.nav.DialognavRenderer" />

<h2><bean:message key="kickstart.script.header1"/></h2>

<div>
  <p>
    <bean:message key="kickstart.script.summary"/>
  </p>
    <html:form method="post" action="/kickstart/KickstartScriptCreate.do">
      <%@ include file="script-form.jspf" %>      
      <html:hidden property="ksid" value="${ksdata.id}"/>
      <html:hidden property="submitted" value="true"/>
    </html:form>
</div>

</body>
</html:html>


<%@ page import="development.RemoteTest" %>



<div class="fieldcontain ${hasErrors(bean: remoteTest, field: 'otherName', 'error')} required">
	<label for="otherName">
		<g:message code="remoteTest.otherName.label" default="Other Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="otherName" required="" value="${remoteTest?.otherName}"/>

</div>


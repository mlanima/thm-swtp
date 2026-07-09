<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
  <#if section = "header">
    <h2 class="auth-title">${msg("errorTitle")}</h2>
  <#elseif section = "form">
    <div class="alert alert-error" role="alert">
      ${kcSanitize(message.summary)?no_esc}
    </div>
    <#if client?? && client.baseUrl?has_content>
      <a href="${client.baseUrl}" class="btn-secondary">
        ${msg("backToApplication")}
      </a>
    </#if>
  </#if>
</@layout.registrationLayout>

<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=true; section>
  <#if section = "header">
    <h2 class="auth-title">${msg("infoReqEmailHeader")!"Information"}</h2>
  <#elseif section = "form">
    <div class="alert alert-success" role="alert">
      ${kcSanitize(message.summary)?no_esc}
    </div>
    <#if requiredActions??>
      <ul class="info-actions-list">
        <#list requiredActions as reqActionItem>
          <li>${msg("requiredAction.${reqActionItem}")}</li>
        </#list>
      </ul>
    </#if>
  <#elseif section = "info">
    <#if skipLink??>
    <#else>
      <#if pageRedirectUri?has_content>
        <p class="auth-info-text">
          <a href="${pageRedirectUri}" class="form-link">${msg("backToApplication")}</a>
        </p>
      <#elseif actionUri?has_content>
        <p class="auth-info-text">
          <a href="${actionUri}" class="form-link">${msg("proceedWithAction")}</a>
        </p>
      <#elseif client.baseUrl?has_content>
        <p class="auth-info-text">
          <a href="${client.baseUrl}" class="form-link">${msg("backToApplication")}</a>
        </p>
      </#if>
    </#if>
  </#if>
</@layout.registrationLayout>

<#import "template.ftl" as layout>
<@layout.registrationLayout
  displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm')
  displayInfo=true; section>

  <#if section = "header">
    <h2 class="auth-title">${msg("registerTitle")}</h2>
  <#elseif section = "form">

    <form id="kc-register-form" action="${url.registrationAction}" method="post">

      <#list profile.attributes as attribute>
        <#if !attribute.readOnly>
          <div class="form-group">
            <label for="${attribute.name}" class="form-label">
              ${advancedMsg(attribute.displayName!'')}
              <#if attribute.required><span class="required-mark">*</span></#if>
            </label>

            <#assign inputType = "text">
            <#if attribute.name == "password" || attribute.name == "password-confirm">
              <#assign inputType = "password">
            <#elseif attribute.name == "email">
              <#assign inputType = "email">
            </#if>

            <input
              id="${attribute.name}"
              name="${attribute.name}"
              type="${inputType}"
              value="<#if attribute.name != 'password' && attribute.name != 'password-confirm'>${(attribute.value!'')}</#if>"
              <#if attribute.name == "firstName" || attribute.name == "username">autofocus</#if>
              <#if attribute.required>required</#if>
              autocomplete="<#if attribute.name == 'password'>new-password<#elseif attribute.name == 'password-confirm'>new-password<#elseif attribute.name == 'email'>email<#elseif attribute.name == 'username'>username<#else>off</#if>"
              class="form-input<#if messagesPerField.existsError(attribute.name)> form-input-error</#if>"
            />

            <#if messagesPerField.existsError(attribute.name)>
              <p class="field-error">${kcSanitize(messagesPerField.getFirstError(attribute.name))?no_esc}</p>
            </#if>
          </div>
        </#if>
      </#list>

      <button type="submit" class="btn-primary">
        ${msg("doRegister")}
      </button>

    </form>

  <#elseif section = "info">
    <p class="auth-info-text">
      ${msg("alreadyHaveAnAccount")}
      <a href="${url.loginUrl}" class="form-link">${msg("doLogIn")}</a>
    </p>
  </#if>

</@layout.registrationLayout>

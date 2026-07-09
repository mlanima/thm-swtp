<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=true; section>

  <#if section = "header">
    <h2 class="auth-title">${msg("registerTitle")}</h2>
  <#elseif section = "form">

    <form id="kc-register-form" action="${url.registrationAction}" method="post">

      <#list profile.attributes as attribute>
        <div class="form-group">
          <label class="form-label" for="${attribute.name}">
            ${advancedMsg(attribute.displayName!'')}
            <#if attribute.required><span class="required-mark">*</span></#if>
          </label>
          <input
            type="${attribute.inputType!'text'}"
            id="${attribute.name}"
            name="${attribute.name}"
            value="${(attribute.value!'')}"
            <#if attribute?is_first>autofocus</#if>
            <#if attribute.readOnly!false>disabled</#if>
            class="form-input<#if messagesPerField.existsError(attribute.name)> form-input-error</#if>"
          />
          <#if messagesPerField.existsError(attribute.name)>
            <p class="field-error">${kcSanitize(messagesPerField.getFirstError(attribute.name))?no_esc}</p>
          </#if>
        </div>
      </#list>

      <#if passwordRequired??>
        <div class="form-group">
          <label class="form-label" for="password">${msg("password")} <span class="required-mark">*</span></label>
          <input
            type="password"
            id="password"
            name="password"
            autocomplete="new-password"
            class="form-input<#if messagesPerField.existsError('password','password-confirm')> form-input-error</#if>"
          />
          <#if messagesPerField.existsError('password')>
            <p class="field-error">${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}</p>
          </#if>
        </div>

        <div class="form-group">
          <label class="form-label" for="password-confirm">${msg("passwordConfirm")} <span class="required-mark">*</span></label>
          <input
            type="password"
            id="password-confirm"
            name="password-confirm"
            autocomplete="new-password"
            class="form-input<#if messagesPerField.existsError('password-confirm')> form-input-error</#if>"
          />
          <#if messagesPerField.existsError('password-confirm')>
            <p class="field-error">${kcSanitize(messagesPerField.getFirstError('password-confirm'))?no_esc}</p>
          </#if>
        </div>
      </#if>

      <button type="submit" class="btn-primary">${msg("doRegister")}</button>

    </form>

  <#elseif section = "info">
    <p class="auth-info-text">
      <a class="form-link" href="${url.loginUrl}">${msg("backToLogin")?no_esc}</a>
    </p>
  </#if>

</@layout.registrationLayout>

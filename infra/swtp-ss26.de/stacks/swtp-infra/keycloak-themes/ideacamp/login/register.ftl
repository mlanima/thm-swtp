<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=true; section>

  <#if section = "header">
    <h2 class="auth-title">${msg("registerTitle")}</h2>
  <#elseif section = "form">

    <form id="kc-register-form" action="${url.registrationAction}" method="post">

      <div class="form-group">
        <label class="form-label" for="firstName">${msg("firstName")} <span class="required-mark">*</span></label>
        <input
          type="text"
          id="firstName"
          name="firstName"
          value="${(register.formData['firstName']!'')}"
          autocomplete="given-name"
          autofocus
          class="form-input<#if messagesPerField.existsError('firstName')> form-input-error</#if>"
        />
        <#if messagesPerField.existsError('firstName')>
          <p class="field-error">${kcSanitize(messagesPerField.getFirstError('firstName'))?no_esc}</p>
        </#if>
      </div>

      <div class="form-group">
        <label class="form-label" for="lastName">${msg("lastName")} <span class="required-mark">*</span></label>
        <input
          type="text"
          id="lastName"
          name="lastName"
          value="${(register.formData['lastName']!'')}"
          autocomplete="family-name"
          class="form-input<#if messagesPerField.existsError('lastName')> form-input-error</#if>"
        />
        <#if messagesPerField.existsError('lastName')>
          <p class="field-error">${kcSanitize(messagesPerField.getFirstError('lastName'))?no_esc}</p>
        </#if>
      </div>

      <#if !realm.registrationEmailAsUsername>
        <div class="form-group">
          <label class="form-label" for="username">${msg("username")} <span class="required-mark">*</span></label>
          <input
            type="text"
            id="username"
            name="username"
            value="${(register.formData['username']!'')}"
            autocomplete="username"
            class="form-input<#if messagesPerField.existsError('username')> form-input-error</#if>"
          />
          <#if messagesPerField.existsError('username')>
            <p class="field-error">${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}</p>
          </#if>
        </div>
      </#if>

      <div class="form-group">
        <label class="form-label" for="email">${msg("email")} <span class="required-mark">*</span></label>
        <input
          type="email"
          id="email"
          name="email"
          value="${(register.formData['email']!'')}"
          autocomplete="email"
          class="form-input<#if messagesPerField.existsError('email')> form-input-error</#if>"
        />
        <#if messagesPerField.existsError('email')>
          <p class="field-error">${kcSanitize(messagesPerField.getFirstError('email'))?no_esc}</p>
        </#if>
      </div>

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

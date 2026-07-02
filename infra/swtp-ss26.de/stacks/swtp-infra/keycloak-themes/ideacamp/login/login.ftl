<#import "template.ftl" as layout>
<@layout.registrationLayout
  displayMessage=!messagesPerField.existsError('username','password')
  displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>

  <#if section = "header">
    <h2 class="auth-title">${msg("loginAccountTitle")}</h2>
  <#elseif section = "form">

    <#if realm.password>
      <form id="kc-form-login" action="${url.loginAction}" method="post">

        <#if !usernameHidden??>
          <div class="form-group">
            <div class="form-float">
              <input
                id="username"
                name="username"
                type="text"
                autocomplete="username"
                value="${(login.username!'')}"
                placeholder=" "
                autofocus
                class="form-input form-input-float<#if messagesPerField.existsError('username','password')> form-input-error</#if>"
              />
              <label for="username" class="form-label-float">
                <#if !realm.loginWithEmailAllowed>
                  ${msg("username")}
                <#elseif !realm.registrationEmailAsUsername>
                  ${msg("usernameOrEmail")}
                <#else>
                  ${msg("email")}
                </#if>
              </label>
            </div>
            <#if messagesPerField.existsError('username')>
              <p class="field-error">${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}</p>
            </#if>
          </div>
        </#if>

        <div class="form-group">
          <div class="form-float">
            <input
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder=" "
              class="form-input form-input-float<#if messagesPerField.existsError('username','password')> form-input-error</#if>"
            />
            <label for="password" class="form-label-float">${msg("password")}</label>
          </div>
          <#if messagesPerField.existsError('password')>
            <p class="field-error">${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}</p>
          </#if>
          <#if realm.resetPasswordAllowed>
            <a href="${url.loginResetCredentialsUrl}" class="form-link form-link-sm forgot-password-link">${msg("doForgotPassword")}</a>
          </#if>
        </div>

        <#if realm.rememberMe && !usernameHidden??>
          <div class="form-group-inline">
            <input
              id="rememberMe"
              name="rememberMe"
              type="checkbox"
              class="form-checkbox"
              <#if login.rememberMe??>checked</#if>
            />
            <label for="rememberMe" class="form-label-inline">${msg("rememberMe")}</label>
          </div>
        </#if>

        <input type="hidden" id="id-hidden-input" name="credentialId"
          value="<#if auth.selectedCredential?has_content>${auth.selectedCredential}</#if>" />

        <button type="submit" class="btn-primary">
          ${msg("doLogIn")}
        </button>

      </form>
    </#if>

  <#elseif section = "info">
    <p class="auth-info-text">
      ${msg("noAccount")}
      <a href="${url.registrationUrl}" class="form-link">${msg("doRegister")}</a>
    </p>
  </#if>

</@layout.registrationLayout>

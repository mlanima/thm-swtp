<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=true; section>

  <#if section = "header">
    <h2 class="auth-title">${msg("emailForgotTitle")}</h2>
  <#elseif section = "form">

    <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
      <div class="form-group">
        <label class="form-label" for="username">
          <#if !realm.loginWithEmailAllowed>${msg("username")}
          <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
          <#else>${msg("email")}
          </#if>
        </label>
        <input
          type="text"
          id="username"
          name="username"
          autofocus
          value="${(auth.attemptedUsername!'')}"
          autocomplete="username"
          class="form-input"
        />
      </div>

      <div class="form-group">
        <button type="submit" class="btn-primary">${msg("doSubmit")}</button>
      </div>
    </form>

  <#elseif section = "info">
    <p class="auth-info-text">
      <a class="form-link" href="${url.loginUrl}">← Back to Login</a>
    </p>
  </#if>

</@layout.registrationLayout>

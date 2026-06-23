<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html lang="${(locale.currentLanguageTag)!'en'}">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>IdeaCamp</title>
  <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
<body class="${bodyClass}">
  <main class="auth-root">
    <div class="auth-card">

      <div class="auth-card-header">
        <div class="logo">
          <img src="${url.resourcesPath}/img/THM_Logo.svg" alt="THM Logo" class="logo-img" />
          <div class="logo-divider"></div>
          <span class="logo-name">IdeaCamp</span>
        </div>
        <#if realm.internationalizationEnabled && locale.supported?size gt 1>
          <div class="locale-switcher">
            <#list locale.supported as l>
              <#if l.languageTag = locale.currentLanguageTag>
                <span class="locale-item locale-item-active">${l.label}</span>
              <#else>
                <a class="locale-item" href="${l.url}">${l.label}</a>
              </#if>
            </#list>
          </div>
        </#if>
      </div>

      <div class="auth-card-body">

        <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
          <div class="alert alert-${message.type}" role="alert">
            ${kcSanitize(message.summary)?no_esc}
          </div>
        </#if>

        <#nested "header">
        <#nested "form">

      </div>

      <#if displayInfo>
        <div class="auth-card-footer">
          <#nested "info">
        </div>
      </#if>

    </div>
  </main>
</body>
</html>
</#macro>

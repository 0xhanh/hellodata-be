window.environment = {
    production: false,
    // portalApi: 'http://localhost:8081/api',
    portalApi: 'http://192.168.51.168:8081/api',
    docsApi: 'http://192.168.51.168:8086/dbt-docs',
    debugInfoEnabled: true,
    authConfig: {
      logLevel: 1,
      authority: 'http://192.168.51.168:38080/realms/hellodata',
      // redirectUrl: 'http://localhost:8080/app/',
      redirectUrl: 'http://192.168.51.168:8080/app/',
      postLogoutRedirectUri: 'http://192.168.51.168:8080/app/',
      clientId: 'frontend-client',
      scope: 'openid profile email offline_access'
    },
    domainNamespace: 'hellodata',
    baseDomain: 'localhost',
    deploymentEnvironment: {
      name: 'DEV',
      headerColor: 'rgb(9,106,232)'
    },
    locale: 'de-CH',
    subSystemsConfig: {
      airflow: {protocol: 'http://', host: 'localhost', domain: ':28080'},
      dbtDocs: {protocol: 'http://', host: 'localhost', domain: ':8086/dbt-docs'},
      dmViewer: {protocol: 'http://', host: 'localhost', domain: ':8087/cloudbeaver/'},
      dwhViewer: {protocol: 'http://', host: 'localhost', domain: ':8087/cloudbeaver/'},
      advancedAnalyticsViewer: {protocol: 'http://', host: 'localhost', domain: ':8088'},
      filebrowser: {protocol: 'http://', host: 'localhost', domain: ':8090'},
      monitoringStatus: {protocol: 'http://', host: 'localhost', domain: ':5099'},
      devToolsMailbox: {protocol: 'http://', host: 'localhost', domain: ':8001'},
      devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8090'},
      dataGov: {protocol: 'http://', host: '192.168.51.165', domain: ':8585'}
    },
    footerConfig: {
      openSourceDataPlatformUrl: 'https://kanton-bern.github.io/hellodata-be',
      licenseUrl: 'https://github.com/kanton-bern/hellodata-be/blob/main/LICENSE',
      githubUrl: 'https://github.com/kanton-bern/hellodata-be',
      versionLink: 'https://github.com/kanton-bern/hellodata-be/releases/'
    }
  }
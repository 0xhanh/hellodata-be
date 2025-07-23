window.environment = {
    production: false,
    portalApi: 'https://192.168.51.168:8080/api',
    docsApi: 'https://192.168.51.168:8080/dbt-docs',
    debugInfoEnabled: true,
    authConfig: {
      logLevel: 1,
      // authority: 'http://192.168.51.168:38080/realms/hellodata',
      authority: 'http://192.168.51.168:38080/realms/hellodata',
      redirectUrl: 'https://192.168.51.168:8080/app/',
      postLogoutRedirectUri: 'https://192.168.51.168:8080/app/',
      clientId: 'frontend-client',
      scope: 'openid profile email offline_access'
    },
    domainNamespace: 'hellodata',
    baseDomain: '192.168.51.168',
    deploymentEnvironment: {
      name: 'DEV',
      headerColor: 'rgb(9,106,232)'
    },
    locale: 'vi',
    subSystemsConfig: {
      airflow: {protocol: 'https://', host: '192.168.51.168', domain: ':8080/airflow'},
      dbtDocs: {protocol: 'https://', host: '192.168.51.168', domain: ':8080/dbt-docs'},
      dmViewer: {protocol: 'https://', host: '192.168.51.168', domain: ':8080/cloudbeaver/'},
      dwhViewer: {protocol: 'https://', host: '192.168.51.168', domain: ':8080/cloudbeaver/'},
      advancedAnalyticsViewer: {protocol: 'https://', host: '192.168.51.168', domain: ':8080'},
      // filebrowser: {protocol: 'http://', host: 'localhost', domain: ':8080/filebrowser'},
      filebrowser: {protocol: 'http://', host: '192.168.51.168', domain: ':8090'},
      monitoringStatus: {protocol: 'http://', host: '192.168.51.168', domain: ':5099'},
      // monitoringStatus: {protocol: 'http://', host: 'localhost', domain: ':8080/ms_status/'},
      // devToolsMailbox: {protocol: 'http://', host: 'localhost', domain: ':8080/mailbox'},
      devToolsMailbox: {protocol: 'http://', host: '192.168.51.168', domain: ':8001'},
      // devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8080'},
      devToolsFileBrowser: {protocol: 'http://', host: '192.168.51.168', domain: ':8090'},
      dataGov: {protocol: 'https://', host: '192.168.51.165', domain: ':8585'}
    },
    footerConfig: {
      openSourceDataPlatformUrl: 'https://kanton-bern.github.io/hellodata-be',
      licenseUrl: 'https://github.com/kanton-bern/hellodata-be/blob/main/LICENSE',
      githubUrl: 'https://github.com/kanton-bern/hellodata-be',
      versionLink: 'https://github.com/kanton-bern/hellodata-be/releases/'
    }
  }
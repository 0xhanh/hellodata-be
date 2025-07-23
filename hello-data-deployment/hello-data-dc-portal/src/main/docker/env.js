window.environment = {
    production: false,
    portalApi: 'http://localhost:8080/api',
    docsApi: 'http://localhost:8080/dbt-docs',
    debugInfoEnabled: true,
    authConfig: {
      logLevel: 1,
      authority: 'http://localhost:8080/auth/realms/hellodata',
      redirectUrl: 'http://localhost:8080/app/',
      postLogoutRedirectUri: 'http://localhost:8080/app/',
      clientId: 'frontend-client',
      scope: 'openid profile email offline_access'
    },
    domainNamespace: 'hellodata',
    baseDomain: 'localhost',
    deploymentEnvironment: {
      name: 'PREVIEW',
      headerColor: 'rgb(9,106,232)'
    },
    locale: 'de-CH',
    subSystemsConfig: {
      airflow: {protocol: 'http://', host: 'localhost', domain: ':8080/airflow'},
      dbtDocs: {protocol: 'http://', host: 'localhost', domain: ':8080/dbt-docs'},
      dmViewer: {protocol: 'http://', host: 'localhost', domain: ':8080/cloudbeaver/'},
      dwhViewer: {protocol: 'http://', host: 'localhost', domain: ':8080/cloudbeaver/'},
      advancedAnalyticsViewer: {protocol: 'http://', host: 'localhost', domain: ':8080/jupyter'},
      filebrowser: {protocol: 'http://', host: 'localhost', domain: ':8080/filebrowser'},
      monitoringStatus: {protocol: 'http://', host: 'localhost', domain: ':8080/status'},
      devToolsMailbox: {protocol: 'http://', host: 'localhost', domain: ':8080/mailbox'},
      devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8080/filebrowser'},
      dataGov: {protocol: 'http://', host: 'localhost', domain: ':8080/datagov'}
    }
}

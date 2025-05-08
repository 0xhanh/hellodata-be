window.environment = {
    production: false,
    portalApi: 'http://192.168.51.168:8080/api',
    docsApi: 'http://192.168.51.168:8080/dbt-docs',
    debugInfoEnabled: true,
    authConfig: {
      logLevel: 1,
      authority: 'http://192.168.51.168:38080/realms/hellodata',
      redirectUrl: 'http://192.168.51.168:8080/app/',
      postLogoutRedirectUri: 'http://192.168.51.168:8080/app/',
      clientId: 'frontend-client',
      scope: 'openid profile email offline_access',
      requireHttps: false, // Tắt yêu cầu HTTPS
      showDebugInformation: true, // Hữu ích để debug
      disableAtHashCheck: true, // Tùy chọn, nếu gặp lỗi liên quan
      sessionChecksEnabled: true,
      silentRefreshTimeout: 5000,
      // Silent refresh (dùng iframe) và session check có thể không hoạt động tốt qua HTTP do hạn chế bảo mật. Bạn có thể tạm thời tắt chúng trong môi trường phát triển:
      // sessionChecksEnabled: false, // Tắt session check
      // silentRefreshTimeout: 0, // Tắt silent refresh
    },
    domainNamespace: 'hellodata',
    baseDomain: '192.168.51.168',
    deploymentEnvironment: {
      name: 'DEV',
      headerColor: 'rgb(9,106,232)'
    },
    locale: 'vi',
    subSystemsConfig: {
      airflow: {protocol: 'http://', host: '192.168.51.168', domain: ':8080/airflow'},
      dbtDocs: {protocol: 'http://', host: '192.168.51.168', domain: ':8080/dbt-docs'},
      dmViewer: {protocol: 'http://', host: '192.168.51.168', domain: ':8080/cloudbeaver/'},
      dwhViewer: {protocol: 'http://', host: '192.168.51.168', domain: ':8080/cloudbeaver/'},
      advancedAnalyticsViewer: {protocol: 'http://', host: '192.168.51.168', domain: ':8080'},
      // filebrowser: {protocol: 'http://', host: 'localhost', domain: ':8080/filebrowser'},
      filebrowser: {protocol: 'http://', host: '192.168.51.168', domain: ':8090'},
      monitoringStatus: {protocol: 'http://', host: '192.168.51.168', domain: ':5099'},
      // monitoringStatus: {protocol: 'http://', host: 'localhost', domain: ':8080/ms_status/'},
      // devToolsMailbox: {protocol: 'http://', host: 'localhost', domain: ':8080/mailbox'},
      devToolsMailbox: {protocol: 'http://', host: '192.168.51.168', domain: ':8001'},
      // devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8080'},
      devToolsFileBrowser: {protocol: 'http://', host: '192.168.51.168', domain: ':8090'},
      dataGov: {protocol: 'http://', host: '192.168.51.168', domain: ':8080/datagov'},
    },
    footerConfig: {
      openSourceDataPlatformUrl: 'http://kanton-bern.github.io/hellodata-be',
      licenseUrl: 'http://github.com/kanton-bern/hellodata-be/blob/main/LICENSE',
      githubUrl: 'http://github.com/kanton-bern/hellodata-be',
      versionLink: 'http://github.com/kanton-bern/hellodata-be/releases/'
    }
  }
clean_up:
	docker rmi bedag/hello-data-portal-ui:latest

image:
	mvn clean install -Pdocker -DskipTests

deploy:
	docker save bedag/hello-data-portal-ui:latest | gzip > hello-data-portal-ui.tar.gz
	scp hello-data-portal-ui.tar.gz hanhvd@ai168:~/dev_ui/
	rm -f hello-data-portal-ui.tar.gz
clean_up:
	docker rmi bedag/hello-data-portal-ui:latest

image:
	mvn clean install -Pdocker -DskipTests

deploy:
	docker save bedag/hello-data-portal-ui:latest | gzip > hello-data-portal-ui.tar.gz
	rm -f hello-data-portal-ui.tar.gz

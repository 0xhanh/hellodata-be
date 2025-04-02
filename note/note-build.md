1. Chuẩn bị release version
   mvn versions:set -DnewVersion=latest
   mvn versions:commit
2. Build và package
   mvn clean package ( mvn clean package -DskipTests )
   mvn install -DskipTests
- Build riêng module: 
  mvn clean package -pl hello-data-portal -am
- Profile đặc biệt:
  mvn clean package -Pproduction
3. Build Docker image và push (nếu cần)
   mvn verify docker:build docker:push
- Bỏ qua module khi build:
  mvn -pl '!hello-data-subsystems/hello-data-monitoring' verify docker:build
  mvn -pl '!hello-data-subsystems/hello-data-monitoring' <goal>
  mvn verify docker:build -DskipModules=hello-data-monitoring
  mvn docker:tag -Ddocker.image.tag=0.9.0
- Build riêng module:
  mvn -pl hello-data-subsystems/hello-data-monitoring verify docker:build
  mvn -pl hello-data-subsystems/hello-data-cloudbeaver-gateway verify docker:build

4. Tạo Git tag và quay lại SNAPSHOT version
   git tag -a v1.3.8.rc1 -m "Release version v1.3.8.rc1"
   git push origin v1.3.8.rc1
   - Quay lại version SNAPSHOT
   mvn versions:set -DnewVersion=develop-SNAPSHOT
   mvn versions:commit
   
5. Docker push
- https://dmp.fabric8.io/#docker:push
   mvn dockerfile:push

## fix lỗi khi build docker image
- khong quay lai dc "develop-SNAPSHOT" de build va package & docker. 
- Fix: vao file .git-versioned-pom.xml chinh lai version: <version>develop-SNAPSHOT</version>


mvn -pl hello-data-sidecars/hello-data-sidecar-portal verify docker:build
mvn -pl hello-data-sidecars/hello-data-sidecar-portal verify docker:build -Ddocker.tag.name=latest
or cd vao thu muon build va chay: mvn clean install -Pdocker

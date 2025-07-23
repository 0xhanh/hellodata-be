#
# Copyright © 2024, Kanton Bern
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of the <organization> nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

# FROM hanhx/hello-data-airflow:2.8.1
FROM hanhx/hello-data-airflow:latest

USER root
# Install dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    gnupg2 \
    unixodbc-dev \
    libgssapi-krb5-2 \
    git \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Check Debian version (11 or 12 supported)
RUN if ! [[ "11 12" == *"$(grep VERSION_ID /etc/os-release | cut -d '"' -f 2 | cut -d '.' -f 1)"* ]]; \
    then echo "Debian $(grep VERSION_ID /etc/os-release | cut -d '"' -f 2 | cut -d '.' -f 1) is not currently supported."; \
    exit 1; \
    fi

# Install Microsoft ODBC Driver 18
RUN curl -sSL -O https://packages.microsoft.com/config/debian/$(grep VERSION_ID /etc/os-release | cut -d '"' -f 2 | cut -d '.' -f 1)/packages-microsoft-prod.deb \
    && dpkg -i packages-microsoft-prod.deb \
    && rm packages-microsoft-prod.deb \
    && rm -f /etc/apt/sources.list.d/mssql-release.list \
    && apt-get update \
    && ACCEPT_EULA=Y apt-get install -y msodbcsql18 \
    && ACCEPT_EULA=Y apt-get install -y mssql-tools18 \
    && echo 'export PATH="$PATH:/opt/mssql-tools18/bin"' >> /home/airflow/.bashrc \
    && rm -rf /var/lib/apt/lists/*

USER airflow
# Install Python dependencies
RUN pip install --no-cache-dir apache-airflow-providers-odbc pyodbc

ADD conf/airflow.cfg /opt/airflow/airflow.cfg
ADD conf/webserver_config.py /opt/airflow/webserver_config.py

## Remove dbt packages
RUN pip uninstall dbt-core dbt-postgres dbt-adapters -y
# hvd: Install requirement for biz domain
ADD additional-requirements.txt ./additional-requirements.txt
# RUN pip install --upgrade -r ./additional-requirements.txt
RUN pip install --no-cache-dir -r ./additional-requirements.txt

# hvd: Installl lib-dev packages
ADD lib-dev ./libdev
RUN pip install --no-cache-dir ./libdev/*.whl

ADD db/02_01_add_extra_dd_connection.sql /db/changelog/changelogs/02_01_add_extra_dd_connection.sql
RUN sed -i '/<\/databaseChangeLog>/d' /db/changelog/airflow-changelog.xml

# Append custom dc changeset to the file
RUN echo '<changeSet id="0002_01" author="hellodata" runOnChange="true">' >> /db/changelog/airflow-changelog.xml \
    && echo '    <sqlFile path="changelogs/02_01_add_extra_dd_connection.sql" relativeToChangelogFile="true" splitStatements="false" />' >> /db/changelog/airflow-changelog.xml \
    && echo '</changeSet>' >> /db/changelog/airflow-changelog.xml \
    && echo '</databaseChangeLog>' >> /db/changelog/airflow-changelog.xml

RUN ls -la /db/changelog/changelogs
RUN cat /db/changelog/airflow-changelog.xml



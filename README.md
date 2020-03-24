Ansible playbook for provisioning a [Debezium](https://debezium.io) demo using my [Summit Lab Spring Music application](https://github.com/edeandrea/summit-lab-spring-music/tree/pipeline) as the "monolith". The [Debezium connector](roles/Demo/objects/debezium-connector-config.json) is configured to use the [Outbox Event Router](https://debezium.io/documentation/reference/1.0/configuration/outbox-event-router.html).

The [application](https://github.com/edeandrea/summit-lab-spring-music/tree/pipeline) is a simple Spring Boot application connected to a MySQL database. We'll install a 3 replica Kafka cluster with Kafka connect and then install the [Debezium MySQL connector](https://debezium.io/documentation/reference/1.0/connectors/mysql.html).

The database credentials are stored in a `Secret` and then [mounted into the Kafka Connect cluster](https://strimzi.io/docs/latest/#proc-kafka-connect-mounting-volumes-deployment-configuration-kafka-connect).

The Kafka Broker, [Kafka Connect](https://access.redhat.com/documentation/en-us/red_hat_amq/7.5/html-single/using_amq_streams_on_openshift/index#kafka-connect-str), and [Kafka Bridge](https://access.redhat.com/documentation/en-us/red_hat_amq/7.5/html-single/using_amq_streams_on_openshift/index#kafka-bridge-concepts-str) are all [authenticated via OAuth 2.0](https://access.redhat.com/documentation/en-us/red_hat_amq/7.5/html-single/using_amq_streams_on_openshift/index#assembly-oauth-str). [Red Hat Single Sign-on](https://access.redhat.com/documentation/en-us/red_hat_single_sign-on/7.3) is installed and used as the authorization server. A new realm is automatically created and provisioned.

## Deployed Resource URLs
All the below resource URLs are suffixed with the apps url of the cluster (i.e. for an RHPDS environment, `apps.cluster-##GUID##.##GUID##.example.opentlc.com`).

- OpenShift Console
    - https://console-openshift-console.##CLUSTER_SUFFIX##/k8s/cluster/projects/demo
- [Kafdrop](https://github.com/obsidiandynamics/kafdrop)
    - http://kafdrop-demo.##CLUSTER_SUFFIX##
- [Demo App](https://github.com/edeandrea/summit-lab-spring-music/tree/pipeline)
    - http://spring-music-demo.##CLUSTER_SUFFIX##
- Red Hat Single Sign-on
    - https://secure-sso-demo.##CLUSTER_SUFFIX##
- Prometheus
    - http://prometheus-demo.##CLUSTER_SUFFIX##
- Grafana
    - http://grafana-route-demo.##CLUSTER_SUFFIX##

## Running the playbook
To run this you would do something like
```bash
$ ansible-playbook -vvv main.yml -e ocp_api_url=<OCP_API_URL> -e ocp_admin_pwd=<OCP_ADMIN_USER_PASSWORD>
```

You'll need to replace the following variables with appropriate values:

| Variable | Description |
| -------- | ----------- |
| `<OCP_API_URL>` | API url of your cluster |
| `<OCP_ADMIN_USER_PASSWORD>` | Password for the OCP admin account |

This playbook also makes some assumptions about some things within the cluster. These variables can be overridden with the `-e` switch when running the playbook.

| Description | Variable | Default Value |
| ----------- | -------- | ------------- |
| OpenShift admin user name | `ocp_admin` | `opentlc-mgr` |
| OCP user to install demo into | `ocp_proj_user` | `user1` |
| OCP user password for above user | `ocp_proj_user_pwd` | `openshift` |
| Project name to install demo into | `proj_nm_demo` | `demo` |

## Additional Resources
- [MySQL Database Template](https://github.com/edeandrea/summit-lab-spring-music/blob/pipeline/misc/templates/prod-template-ocp4.yml)
- [AMQ Streams Template](roles/Demo/templates/amq-streams-template.yml.j2)
    - Includes `Kafka`, `KafkaConnect`, `KafkaConnector`, and `KafkaBridge` custom resources
- [Kafdrop Template](roles/Demo/objects/kafdrop.yml)
- [Red Hat SSO Realm Config](roles/Demo/objects/spring-music-cdc-realm.json)
- [Guide used](https://github.com/sigreen/amq-streams-oauth-ldap) for help in setting this all up
    - Thanks @sigreen!

## Backlog for enhancements
PRs welcome!
- Enabling schema registry and using AVRO serializtion/deserialization
- Adding metrics to `KafkaConnect` & `KafkaBridge` as well as adding dashboards to Grafana
- Adding authorization on the topics to the different clients
- Getting Kafdrop to authenticate with the broker
    - This will allow removal of the `plain` listener on the broker
- Integrate the `KafkaBridge` with something (3scale?)

# Exemplos de requests
# curl -v -XPOST -H "content-type: application/json" -d '{"apelido" : "xpto", "nome" : "xpto xpto", "nascimento" : "2000-01-01", "stack": null}' "http://localhost:9999/pessoas"
# curl -v -XGET "http://localhost:9999/pessoas/1"
# curl -v -XGET "http://localhost:9999/pessoas?t=xpto"
# curl -v "http://localhost:9999/contagem-pessoas"

GATLING_BIN_DIR=$HOME/Development/gatling/3.9.5/bin


sh $GATLING_BIN_DIR/gatling.sh -rm local -s RinhaBackendSimulation \
    -rd "DESCRICAO" \
    -rf $PWD/user-files/results \
    -sf $PWD/user-files/simulations \
    -rsf $PWD/user-files/resources \

sleep 3

curl -v "http://localhost:9999/contagem-pessoas"

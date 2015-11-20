# Relatório 4 - ESOF #
## Verificação e Validação de Software ##
### Introdução

Este relatório tem como objetivo a análise dos processos de verificação e validação (V&V) no desenvolvimento da aplicação Android WordPress. 

Iremos explorar a testabilidade do software. Será analisado grau controlabilidade, de observabilidade,  de isolabilidade, de separação, de inteligibilidade e de heterogeneidade associados aos componenetes do projeto. Por fim, serão apresentadas estatísticas reveladoras da verificação e validação do software.
Esta avaliação será acompanhada de exemplos e de referências que suportem a nossa interpretação.

### Testabilidade do Software

Iremos agora avaliar o quão testável o projeto WordPress para Android é, isto é, até que ponto é possível verificar e validar o mesmo em termos de implementação. É importante salientar que depois de analisado o código e lido algumas especificações podémos constatar que o projeto utiliza a [Android Testing Framework] (http://developer.android.com/intl/ko/tools/testing/testing_android.html) que proporciona uma arquitetura bem como um conjunto de ferramentas que permitem testar todos os aspetos da aplicação, através de testes unitários.

![test_framework](http://developer.android.com/images/testing/test_framework.png)


#### Controlabilidade

A extensão [Android JUnit] (http://developer.android.com/intl/ko/tools/testing/testing_android.html#JUnit) proporciona  classes especificas para os casos de teste. Estas classes proporcionam métodos de auxilio para criar *mock objects* e métodos para controlar o ciclo de vida de um componente.
As ferramentas proporcionadas são um conjunto de métodos ou *"hooks"* do sistema Android. Estes *"hooks"* controlam cada componenete independentemente do seu ciclo de vida normal bem como o *loading* de aplicações.

É ainda de salientar que é utilizada a ferramente [Travis](https://travis-ci.org/) que permite sincronizar o projeto (existente no GitHub) com o Travis que, por sua vez, testa o codigo rápidamente. Os testes são realizados ao nível dos *packages* e permite correr testes em paralelo, bom como ver os testes em tempo real e detalhadamente.


#### Observabilidade
Como já foi referido anteriormente, as duas ferramentas usadas no projeto WordPRess para Android, para efeitos de teste, são o a extenção Android JUnit, para testes unitários, e o Travis, para testes de integração.
A extensão Android JUnit contem um conjunto de ferramentas que facilitam os testes. Em termos de observabilidade a extensão proporciona uma class *Observable* que permite notificar alterações nas classes, ou seja qualquer alteração nas propriedades da mesma. Existem ainda a classe *TestListener* e outras classes que facilitam a observabilidade dos testes pois permitem num estado intermédio e final verificar isoladamente os resultados. Permite ainda a execução de testes em processos paralelos, o que resulta num melhor desempenho.
Por outro lado a ferramenta Travis sujeita os pull requests a vários testes automatizados definidos, de forma a possibilitar a adição, sem conflitos das novas funcionalidades. Estes testes não são testes de aceitação, mas antes testes de integração sobre o código submetido. Na página da ferramenta, é possível ver o resultado dos testes realizados sobre os pull requests submetidos ao projeto do React.


#### Isolabilidade



#### Separação



#### Inteligibilidade



#### Heterogeneidade



### Estatísticas de Teste



### Autores



* Fábio Amarante
* Luís Gonçalves
* Ricardo Lopes


## Contribuição

Relativamente à contribuição de cada elemento para a realização deste relatório, o grupo considera que o trabalho foi igualmente dividido pelos três elementos.

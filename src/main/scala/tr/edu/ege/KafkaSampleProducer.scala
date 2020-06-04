package tr.edu.ege

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import tr.edu.ege.actors.Publisher

object KafkaSampleProducer extends App {

  val producer = new KafkaProducer[String, String](Publisher.producerProperties)
  val topic: String = "aca759de-e30f-4ee0-811d-af35b2957e7c"
  val key: String = "|root|"
  //<eSearchResult><Count>56</Count><RetMax>20</RetMax><RetStart>0</RetStart><IdList><Id>2</Id><Id>3</Id><Id>20046412</Id><Id>20021457</Id><Id>20008883</Id><Id>20008181</Id><Id>19912318</Id><Id>19897276</Id><Id>19895589</Id><Id>19894390</Id><Id>19852204</Id><Id>19839969</Id><Id>19811112</Id><Id>19757309</Id><Id>19749079</Id><Id>19739647</Id><Id>19706339</Id><Id>19665766</Id><Id>19648384</Id><Id>19647860</Id></IdList><TranslationSet><Translation><From>asthma[mesh]</From><To>"asthma"[MeSH Terms]</To></Translation><Translation><From>leukotrienes[mesh]</From><To>"leukotrienes"[MeSH Terms]</To></Translation></TranslationSet><TranslationStack><TermSet><Term>"asthma"[MeSH Terms]</Term><Field>MeSH Terms</Field><Count>125546</Count><Explode>Y</Explode></TermSet><TermSet><Term>"leukotrienes"[MeSH Terms]</Term><Field>MeSH Terms</Field><Count>14178</Count><Explode>Y</Explode></TermSet><OP>AND</OP><TermSet><Term>2009[pdat]</Term><Field>pdat</Field><Count>877017</Count><Explode>N</Explode></TermSet><OP>AND</OP></TranslationStack><QueryTranslation>"asthma"[MeSH Terms] AND "leukotrienes"[MeSH Terms] AND 2009[pdat]</QueryTranslation></eSearchResult>
  val value: String =
    """
      |<eSearchResult><Count>56</Count><RetMax>20</RetMax><RetStart>0</RetStart><IdList><Id>1</Id><Id>2</Id><Id>3</Id></IdList><TranslationSet><Translation><From>asthma[mesh]</From><To>"asthma"[MeSH Terms]</To></Translation><Translation><From>leukotrienes[mesh]</From><To>"leukotrienes"[MeSH Terms]</To></Translation></TranslationSet><TranslationStack><TermSet><Term>"asthma"[MeSH Terms]</Term><Field>MeSH Terms</Field><Count>125546</Count><Explode>Y</Explode></TermSet><TermSet><Term>"leukotrienes"[MeSH Terms]</Term><Field>MeSH Terms</Field><Count>14178</Count><Explode>Y</Explode></TermSet><OP>AND</OP><TermSet><Term>2009[pdat]</Term><Field>pdat</Field><Count>877017</Count><Explode>N</Explode></TermSet><OP>AND</OP></TranslationStack><QueryTranslation>"asthma"[MeSH Terms] AND "leukotrienes"[MeSH Terms] AND 2009[pdat]</QueryTranslation></eSearchResult>
      |""".stripMargin

  println(s"New record publishing...\nTopic:$topic, Key:$key, Value:$value")

  val record = new ProducerRecord[String, String](topic, key, value)
  try {
    producer.send(record).get
  }
  catch {
    case e: Throwable => println("An error occurred while sending record to Apache Kafka.")
  }
  finally {
    producer.close()
  }
}

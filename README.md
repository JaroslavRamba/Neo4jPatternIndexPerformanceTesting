# Neo4jPatternIndexPerformanceTesting


1) dotazovat se nad 1 id nodu a unionovat to pres počet uzlu
   - vylepšení: nad neopakováním se pro dotazovaní nad jedním uzlem, již dotazované uzly neopakovat a rovnou přeskočit
2) to samé, ale dotazovat se přes všechny nody a udělat faktorial počtu nodu = počet unionu
3) vytvoření externí databaze a po jednom patternu to tam šoupat a dotazovat se nad tím, pak to smazat a takhle dokola krom vytvoreni DB
4) všechno to samé jako předchozí ale na začátku vytvořit DB a všechny patterny tam vložit = subgraf patternu a nad tím se pak dotazovat přes všechny zaindexpvané patterny



# Neo4jPatternIndexPerformanceTesting

1) GetTrianglesOriginalTest
   - dotaz pro získání přes trojúhelníků pomocí obyčejného Cypher dotazu
   
2) GetTrianglesWithSingleNodePTest 
   - optimalizace dotazu pro získání všech trojúhelníků - dotazování trojúhelníků přes jeden (ze 3 v trojúhelníku) zakotvený uzel
   
3)GetTrianglesWithSingleNodeOptPTest 
   - optimalizace dotazu pro získání všech trojúhelníků - dotazování trojúhelníků přes jeden (ze 3 v trojúhelníku) zakotvený uzel - každý uzel je dotazován nejvýše jednou (není možné se dotazovat vícekrát s jedním ukotveným uzlem)
   
4) GetTrianglesWithAllNodesPTest
   - optimalizace dotazu pro získání všech trojúhelníků - dotazování trojúhelníků přes všechny (ze 3 v trojúhelníku) zakotvené uzly
   
5) GetTrianglesDBSinglePTest
   - optimalizace dotazu pro získání všech trojúhelníků - nahrání každého trojúhelníku zvlášť do externí databáze a dotazování nad ním

6) GetTrianglesDBAllPTest
   - optimalizace dotazu pro získání všech trojúhelníků - nahrání všech trojúhelníku najednou do externí databáze a dotazování nad nimi



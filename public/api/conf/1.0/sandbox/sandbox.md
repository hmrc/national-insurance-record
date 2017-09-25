<table>
  <thead>
    <tr>
      <th>Scenario</th>
      <th>NINO prefix</th>
      <th>Response</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th>National Insruance record variants</th>
    </tr>
    <tr>
      <td>Taxpayer who has gaps in their record</td>
      <td><code class="code--slim">YN315615A</code></td>
      <td>200 (OK) Payload as regular example above</td>
    </tr>
    <tr>
      <td>Taxpayer who has only gaps in their record</td>
      <td><code class="code--slim">AB216913B</code></td>
      <td>200 (OK) Payload as regular example above</td>
    </tr>
    <tr>
      <td>Taxpayer who has no gaps in their record</td>
      <td><code class="code--slim">PJ523813C</code></td>
      <td>200 (OK) Payload as regular example above</td>
    </tr>
    <tr>
      <td>Taxpayer who has no years at all on their record</td>
      <td><code class="code--slim">NE541113B</code></td>
      <td>200 (OK) Payload as regular example above</td>
    </tr>
    <tr>
      <td>Taxpayer who has Home Responsibilities Protection (HRP)</td>
      <td><code class="code--slim">AA252813D</code></td>
      <td>200 (OK) Payload as regular example above</td>
    </tr>
    <tr>
      <th>Exclusions</th>
    </tr>
    <tr>
      <td>Taxpayer who has Isle of Man liability</td>
      <td><code class="code--slim">MA000002A</code></td>
      <td>403 (Forbidden) {"code": "EXCLUSION_ISLE_OF_MAN"}</td>
    </tr>
    <tr>
      <td>Taxpayer who has the Married Women's Reduced Rate Election flag</td>
      <td><code class="code--slim">EA791213A</code></td>
      <td>403 (Forbidden) {"code": "EXCLUSION_MARRIED_WOMENS_REDUCED_RATE"}</td>
    </tr>
    <tr>
      <td>Taxpayer who has a date of death</td>
      <td><code class="code--slim">ZX000056A</code></td>
      <td>403 (Forbidden) {"code": "EXCLUSION_DEAD"}</td>
    </tr>
    <tr>
      <td>Taxpayer who has the manual correspondence indicator flag</td>
      <td><code class="code--slim">ST281614D</code></td>
      <td>403 (Forbidden) {"code": "EXCLUSION_MANUAL_CORRESPONDENCE"}</td>
    </tr>
  </tbody>
</table>

DB名：todos
ユーザー名：todo
pass：secret

また、それぞれのバージョン情報は下記になります。
Vue.js v2.6.11
SpringBoot v2.7.8
postgreSQL v17

フロントエンド起動
npm run dev

バックエンド起動
.\mvnw.cmd spring-boot:run

初期データとして生成される目標は目標未設定でタスクのみを追加した場合にタスクが追加される場所となっています。(削除不可)


Solr設定
todoApp\solrで実行

Solr起動
bin\solr.cmd start -p 8983

コレクション作成
bin\solr.cmd create -c todoapp

スキーマ(PowerShell)
Setup-SolrSchema.ps1実行でスキーマ設定は完了


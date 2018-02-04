// 全て大文字の識別子はJenkinsのジョブパラメータとして定義する前提
job(DSL_PROJECT_NAME) {
  // リポジトリの設定
  scm {
    git {
      remote {
        url(DSL_GITBUCKET_REPO_URL)
        credentials(DSL_CREDENTAILS)
      }
      branch('*/develop')
    }
  }
  // ビルドのステップ
  steps {
    gradle {
      switches('--refresh-dependencies')
      tasks('clean')
      tasks('build')
      gradleName('Gradle 2.10')
      useWrapper(false)
      makeExecutable(true)
      useWorkspaceAsHome(false)
    }
  }
  // ビルド後の処理
  publishers {
    // JUnitテスト結果の集計
    archiveJunit('**/build/test-results/*.xml') {
      allowEmptyResults(false)
    }
    // Email-ext pluginに送信先とトリガーを設定
    extendedEmail {
      recipientList('$DEFAULT_RECIPIENTS')
      triggers {
        aborted { recipientList(DSL_NOTIFY_LIST) }
        failure { recipientList(DSL_NOTIFY_LIST) }
        unstable { recipientList(DSL_NOTIFY_LIST) }
      }
    }
  }
  // ここからDSLにAPIがない設定
  configure { project ->
    // GitBucketへのpushをビルドトリガーにする
    project / triggers << 'org.jenkinsci.plugins.gitbucket.GitBucketPushTrigger' {
      passThroughGitCommit(false)
    }
    // GitBucketのリポジトリURL（GitリポジトリURLではない）
    project / 'properties' / 'org.jenkinsci.plugins.gitbucket.GitBucketProjectProperty' {
      url(DSL_GITBUCKET_URL)
      linkEnabled(true)
    }
    // ビルドの履歴を10個までしか保持しない
    project / 'properties' / 'jenkins.model.BuildDiscarderProperty' {
      strategy {
        daysToKeep(-1)
        numToKeep(10)
        artifactDaysToKeep(-1)
        artifactNumToKeep(10)
      }
    }
    // JUnitテスト結果集計の空模様
    project / publishers / 'hudson.tasks.junit.JUnitResultArchiver' << {
      healthScaleFactor('1.0')
    }
  }
}

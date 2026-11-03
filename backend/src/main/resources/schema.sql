CREATE TABLE IF NOT EXISTS analyses (
  id UUID PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  member_count INTEGER NOT NULL,
  report TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS analyses_created_at_idx ON analyses (created_at DESC);

CREATE TABLE IF NOT EXISTS tournaments (
 id UUID PRIMARY KEY,
 name VARCHAR(80) NOT NULL,
 season VARCHAR(30) NOT NULL,
 location VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS tournament_teams (
 id UUID PRIMARY KEY,
 tournament_id UUID NOT NULL REFERENCES tournaments(id),
 name VARCHAR(60) NOT NULL,
 name_key VARCHAR(150) NOT NULL,
 short_name VARCHAR(5) NOT NULL,
 color VARCHAR(7) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 UNIQUE (tournament_id,name_key),
 UNIQUE (id,tournament_id)
);
CREATE TABLE IF NOT EXISTS tournament_matches (
 id UUID PRIMARY KEY,
 tournament_id UUID NOT NULL REFERENCES tournaments(id),
 round_number INTEGER NOT NULL CHECK (round_number>0),
 home_id UUID NOT NULL,
 away_id UUID NOT NULL,
 home_goals INTEGER,
 away_goals INTEGER,
 kickoff_at TIMESTAMPTZ,
 field VARCHAR(100),
 field_key VARCHAR(150),
 version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY (home_id,tournament_id) REFERENCES tournament_teams(id,tournament_id),
 FOREIGN KEY (away_id,tournament_id) REFERENCES tournament_teams(id,tournament_id),
 UNIQUE (tournament_id,home_id,away_id),
 CHECK (home_id<>away_id),
 CHECK ((home_goals IS NULL AND away_goals IS NULL) OR (home_goals IS NOT NULL AND away_goals IS NOT NULL AND home_goals BETWEEN 0 AND 99 AND away_goals BETWEEN 0 AND 99))
);
CREATE INDEX IF NOT EXISTS tournament_matches_round_idx ON tournament_matches(tournament_id,round_number);
CREATE INDEX IF NOT EXISTS tournament_matches_schedule_idx ON tournament_matches(kickoff_at) WHERE kickoff_at IS NOT NULL;

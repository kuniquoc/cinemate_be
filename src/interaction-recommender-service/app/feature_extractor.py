class FeatureExtractor:
    def __init__(self, cache):
        self.cache = cache

    async def extract_features(self, data, session):
        # Minimal extractor: update cached features via EventService elsewhere
        user_id = data.get("userId")
        if not user_id:
            return None
        # no-op placeholder
        return True

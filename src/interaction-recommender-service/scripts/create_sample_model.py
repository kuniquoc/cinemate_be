"""
Sample recommendation model generator
Creates a simple model for testing/development
"""
import os
import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier

def create_sample_model():
    """Create a simple sample model for testing"""
    
    # Generate synthetic training data
    np.random.seed(42)
    n_samples = 1000
    n_features = 10
    
    X = np.random.randn(n_samples, n_features)
    y = (X[:, 0] + X[:, 1] > 0).astype(int)
    
    # Train a simple model
    model = RandomForestClassifier(n_estimators=10, random_state=42)
    model.fit(X, y)
    
    # Save model
    models_dir = os.path.join(os.path.dirname(__file__), '..', 'models')
    os.makedirs(models_dir, exist_ok=True)
    
    model_path = os.path.join(models_dir, 'recommender.pkl')
    joblib.dump(model, model_path)
    
    print(f"Sample model saved to: {model_path}")
    return model_path

if __name__ == "__main__":
    create_sample_model()
